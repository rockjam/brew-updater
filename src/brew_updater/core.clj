(ns brew-updater.core
  (:gen-class)
  (:require
    [clojure.data.json :as json]
    [clojure.java.shell :refer [sh]]
    [clojure.repl.deps :refer [sync-deps]]
    [clojure.string :as str]
    [io.github.humbleui.ui :as ui]
    [clojure.java.io :as jio]))

(defn make-cask [{:keys [name token version installed]}]
  (let [clean-version (fn [v] (first (str/split v #",")))
        version (clean-version version)
        installed-version (clean-version installed)]
    {:name              (first name)
     :token             token
     :version           version
     :installed-version installed-version
     :outdated?         (not= version installed-version)}))

(def brew-binary
  (cond
    (jio/file "/opt/homebrew/bin/brew") "/opt/homebrew/bin/brew"
    (jio/file "/usr/local/bin/brew") "/usr/local/bin/brew"
    :else nil))

(defn update-brew []
  (= 0 (:exit (sh brew-binary "update"))))

(defn read-all-casks []
  (let [casks (->
                (sh brew-binary "info" "--installed" "--json=v2")
                :out
                (json/read-str :key-fn keyword)
                :casks)]
    (map make-cask casks)))

(defn read-cask [cask]
  (->
    (sh brew-binary "info" "--cask" "--json=v2" cask)
    :out
    (json/read-str :key-fn keyword)
    :casks
    first
    make-cask))

(comment
  (read-all-casks)

  (read-cask "Alfred")

  (update-brew)

  ())

(defn by-outdated-name [a b]
  (let [by-version (fn [cask] (if (:outdated? cask) -1 1))]
    (compare
      [(by-version a) (:token a)]
      [(by-version b) (:token b)])))

(defonce *state (ui/signal {}))

(defn init-state []
  (let [brew-installed? (some? brew-binary)
        init-casks (fn []
                     (do
                       (update-brew)
                       (into {} (map (fn [x] [(:token x) x]) (read-all-casks)))))
        s (if brew-installed?
            {:state :ready
             :casks (init-casks)}
            {:state :error
             :error "Homebrew not found. Install Homebrew and restart the application."})]
    (reset! *state s)))

(comment
  @*state

  (init-state)

  ())

(defn upgrade-cask [cask]
  (future
    ;; TODO: catch exceptions
    (do
      (println "Updating" cask)
      (sh "brew" "upgrade" "--cask" cask)
      (swap! *state #(update-in % [cask] (fn [_] (read-cask cask))))
      (println cask "updated"))))

(defn label
  ([text]
   (label text {:row-color 0x00000000}))
  ([text opts]
   [ui/rect {:paint {:fill (:row-color opts)}}
    [ui/padding {:padding 10} [ui/label text]]]))

(defn header []
  [ui/row
   (label "Cask")
   (label "Installed Version")
   (label "Latest Version")
   (label "")])

(defn update-button [row-color token]
  [ui/rect {:paint {:fill row-color}}
   [ui/button {:on-click (fn [e] (upgrade-cask token))}
    [ui/label "Update"]]])

(defn cask-row [index {:keys [name token version installed-version] :as cask}]
  (let [row-color (if (odd? index) 0x00000000 0xF5F5F5F5)
        opts {:row-color row-color}
        update-button (if (:outdated? cask) (update-button row-color token) (label "" opts))]
    (list
      (label name opts)
      (label installed-version opts)
      (label version opts)
      update-button)))

(defn ui-app-header [state casks]
  (let [outdated-count (count (filter :outdated? casks))]
    [ui/row
     [ui/column
      (label "Installed Applications")
      (label (str outdated-count " outdated"))
      (label (str "State: " state))]
     [ui/button
      {:on-click (fn [e] (println "Updating all applications"))}
      [ui/label "Update all"]]]))

(defn ui-casks-table [casks]
  [ui/rect {:radius 8 :paint {:fill 0xFFFFFFFF}}
   [ui/rect {:radius 8 :paint {:stroke 0xFFE0E0E0, :width 0.8}}
    [ui/column (header)
     [ui/vscroll
      [ui/grid {:cols 4}
       (map-indexed cask-row casks)]]]]])

(defn app []
  (let [casks-info (->> (:casks @*state)
                        (vals)
                        (sort by-outdated-name))
        state (:state @*state)]
    [ui/align {:x 0.5}
     [ui/column
      (ui-app-header state casks-info)
      (ui-casks-table casks-info)]]))

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
  (init-state)
  (start-app (first args)))
