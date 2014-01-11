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
  (sh "adb" "pull" wa-path))

(fetch-database)

(def msgstore (sqlite3 {:db "msgstore.db"}))
(defdb msg-db msgstore)

(defentity chat_list
  (database msg-db))
(defentity messages
  (database msg-db)
  (entity-fields :_id :data :key_remote_jid
                 :remote_resource
                 :received_timestamp))

(def wa (sqlite3 {:db "wa.db"}))
(defdb wa-db wa)

(defentity wa_contacts
  (database wa-db))

(def msgs
  (select messages
          (fields :_id :data :key_remote_jid
                 [:remote_resource :jid]
                 :received_timestamp)
          (order :received_timestamp :DESC)))

(defn get-name-by-jid [jid]
  (-> (select wa_contacts
          (fields :display_name)
          (where {:jid jid})
          (limit 1))
      (first)
      (:display_name)))

(defn process-msg [{:keys [data jid key_from_me]}]
  (if (= key_from_me 1)
    {:jid jid :data data :name "Gabriele Carrettoni"}
    (let [name (get-name-by-jid jid)]
      {:jid jid :data data :name name})))

(defn save-last-msg-id [msgs]
  (spit "last-msg-id" (:_id (first msgs))))

(defn load-last-msg-id []
  (try
    (Integer/parseInt (slurp "last-msg-id"))
    (catch Exception e
      0)))

(defn get-last-messages [last-id]
  (map process-msg
       (select messages
               (fields [:key_remote_jid :jid] :data :key_from_me)
               (where {:_id [> last-id]}))))

;;(save-last-msg msgs)
;;(load-last-msg-id)
(first msgs)
(get-last-messages (load-last-msg-id))
