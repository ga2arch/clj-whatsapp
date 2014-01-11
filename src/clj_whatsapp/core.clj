(ns clj-whatsapp.core
  (:use korma.db
        korma.core
        [clojure.java.shell :only [sh]]))

;;(def host "192.168.1.3")

(def msgstore-path "/data/data/com.whatsapp/databases/msgstore.db")
(def wa-path "/data/data/com.whatsapp/databases/wa.db")

(defn fetch-database []
  (sh "adb" "root")
  (sh "adb" "pull" msgstore-path)
  (sh "adb" "pull" wa-path)
  (sh "sh" "-c" "sqlite3 msgstore.db .dump | sqlite3 whatsapp.db")
  (sh "sh" "-c" "sqlite3 wa.db .dump | sqlite3 whatsapp.db")
  (sh "rm" "msgstore.db")
  (sh "sh" "-c" "rm wa.db*"))

(fetch-database)

(def wa (sqlite3 {:db "whatsapp.db"}))
(defdb wa-db wa)

(defentity wa_contacts
  (pk :jid)
  (entity-fields :display_name))

(defentity chat_list
  (pk :key_remote_jid)
  (entity-fields :subject))

(defentity messages
  (entity-fields :_id :data
                 :received_timestamp))

(defn get-msgs []
  (select messages
          (fields :data
                  :received_timestamp
                  :wa_contacts.display_name
                  [:key_remote_jid :jid])
          (join wa_contacts (= :wa_contacts.jid :key_remote_jid))
          (where (like :remote_resource ""))
          (order :received_timestamp :DESC)))

(defn get-groups []
  (let [msgs (select messages
                    (fields :data
                            [:remote_resource :user_jid]
                            :received_timestamp
                            [:chat_list.subject :group_name]
                            :wa_contacts.display_name)
                    (join wa_contacts (= :wa_contacts.jid :user_jid))
                    (join chat_list (= :chat_list.key_remote_jid :key_remote_jid))
                    (where (not (like :user_jid "")))
                    (order :received_timestamp :DESC))]
    (reduce (fn [m msg]
              (let [key (keyword (:group_name msg))
                    val (get m key)
                    nmsg (dissoc msg :group_name)]
                (if (contains? m key)
                  (assoc m key (conj val nmsg))
                  (assoc m key [nmsg])))) {} msgs)))

(defn save-last-msg-id [msgs]
  (spit "last-msg-id" (:_id (first msgs))))

(defn load-last-msg-id []
  (try
    (Integer/parseInt (slurp "last-msg-id"))
    (catch Exception e
      0)))

;;(save-last-msg msgs)
;;(load-last-msg-id)
