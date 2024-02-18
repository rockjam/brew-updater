(ns brew-updater.core
  (:gen-class)
  (:require
    [clojure.data.json :as json]
    [clojure.java.shell :refer [sh]]
    [clojure.repl.deps :refer [sync-deps]]
    [clojure.string :as str]
    [clojure.core.async :refer [thread go <!]]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(def caskroom-dir "/opt/homebrew/Caskroom")

(defn brew-update []
  (= 0 (:exit (sh "brew" "update"))))

(defn upgrade-cask [cask]
  (let [ch (thread (sh "brew" "upgrade" "--cask" cask))]
    (do
      (println "Upgrading" cask)
      (go (println (<! ch))))))

(defn list-casks [] (-> (sh "ls" caskroom-dir)
                        (:out)
                        (str/split #"\n")))

(defn read-cask-info [cask]
  (let [command-output (:out (sh "brew" "info" "--cask" "--json=v2" cask))
        cask-info (first (:casks (json/read-str command-output :key-fn keyword)))]
    (select-keys cask-info [:name :token :version :installed])))

(defn all-casks-info []
  (pmap read-cask-info (list-casks)))

(defn by-outdated-name [a b]
  (let [by-version (fn [{:keys [version installed]}] (if (= version installed) 1 -1))]
    (compare
      [(by-version a) (:token a)]
      [(by-version b) (:token b)])))

(def casks-info (sort by-outdated-name (all-casks-info)))

(defn label [text]
  (ui/rect (paint/fill 0x00000000)
           (ui/padding 10 (ui/label text))))

(defn header []
  [(label "Cask") (label "Installed Version") (label "Latest Version") (label "Outdated?")])


(defn cask-row [{:keys [token version installed]}]
  (let [update-button (if (= version installed) (label "") (ui/button #(upgrade-cask token)
                                                                      (ui/label "Update")))]
    [(label token)
     (label installed)
     (label version)
     update-button]))

(def app
  (ui/default-theme
    {}
    (ui/halign 0.5
               (ui/padding
                 0
                 (ui/grid
                   (cons (header) (map cask-row casks-info)))))))

(defn start-app []
  (ui/start-app!
    (ui/window
      {:title "Homebrew Updater"}
      #'app)))

(comment
  (sync-deps)

  (start-app)

  (assoc-in [:compile-opts :direct-linking])

  (System/getProperty "java.version")


  {})

(defn -main []
  (println "hello world")
  (start-app))
