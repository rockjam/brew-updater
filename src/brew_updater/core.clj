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
        {:keys [name token version installed]} (first (:casks (json/read-str command-output :key-fn keyword)))
        clean-version (fn [v] (first (str/split v #",")))]
    {:name              (first name)
     :token             token
     :version           (clean-version version)
     :installed-version (clean-version installed)}))

(first (str/split "4.26.1,131620" #","))

(defn all-casks-info []
  (pmap read-cask-info (list-casks)))

(defn is-outdated? [{:keys [version installed-version]}] (not (= version installed-version)))

(defn by-outdated-name [a b]
  (let [by-version (fn [cask] (if (is-outdated? cask) -1 1))]
    (compare
      [(by-version a) (:token a)]
      [(by-version b) (:token b)])))

(def casks-info (sort by-outdated-name (all-casks-info)))

(defn label
  ([text]
   (label text {:row-color 0x00000000}))
  ([text opts]
   (ui/rect (paint/fill (:row-color opts))
            (ui/padding 10 (ui/label text)))))


(defn header []
  (ui/row
    [:stretch 1 (label "Cask")]
    [:stretch 1 (label "Installed Version")]
    [:stretch 1 (label "Latest Version")]
    [:stretch 1 (label "")]))

(defn cask-row [index {:keys [name token version installed-version] :as cask}]
  (let [row-color (if (odd? index) 0x00000000 0xF5F5F5F5)
        opts {:row-color row-color}
        update-button (if (is-outdated? cask) (ui/rect (paint/fill row-color) (ui/padding 20 0 (ui/button #(upgrade-cask token)
                                                                                                          (ui/label "Update")))) (label "" opts))]
    [(label name opts)
     (label installed-version opts)
     (label version opts)
     update-button]))

(defn ui-app-header [casks]
  (let [outdated-count (count (filter is-outdated? casks))]
    (ui/row
      [:stretch 1
       (ui/column
         (ui/label "Installed Applications")
         (ui/label (str outdated-count " outdated")))]
      [:stretch 1
       (ui/halign 1 1
                  (ui/button
                    #(println "Updating all applications")
                    (ui/label "Update all")))])))


(defn ui-casks-table [casks]
  (ui/rounded-rect
    {:radius 8} (paint/fill 0xFFFFFFFF)
    (ui/rounded-rect
      {:radius 8} (paint/stroke 0xFFE0E0E0 0.8)
      (ui/column (header)
                 (ui/vscrollbar
                   (ui/grid
                     (map-indexed cask-row casks)))))))

(def app
  (ui/default-theme
    {}
    (ui/halign 0.5
               (ui/column
                 (ui-app-header casks-info)
                 (ui-casks-table casks-info)))))

(defn start-app [app-icon]
  (ui/start-app!
    (ui/window
      {:title    "Homebrew Updater"
       :width    600
       :height   600
       :mac-icon app-icon}
      #'app)))

(comment
  (sync-deps)

  (start-app "images/icon.icns")

  {})

(defn -main [& args]
  (println "Starting the application" (first args))
  (start-app (first args)))
