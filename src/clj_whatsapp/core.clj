(ns clj-whatsapp.core
  (:use clj-ssh.ssh
        [clojure.java.shell :only [sh]]))

;;(def host "192.168.1.3")
(sh "adb root && adb pull /data/data/com.whatsapp/databases/msgstore.d")
