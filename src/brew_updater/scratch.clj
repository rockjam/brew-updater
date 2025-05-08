(ns brew-updater.scratch
  (:require [clojure.data.json :as json]
            [clojure.java.io :as jio]
            [clojure.string :as str]))

(def brew-prefix "/opt/homebrew")

(def caskroom (str brew-prefix "/" "Caskroom"))

(defn read-cask-info [cask-path]
  (let [token (.getName cask-path)
        metadata-dir (jio/file cask-path ".metadata")
        clean-version (fn [v] (first (str/split v #",")))
        make-cask (fn [{:keys [name full_token version]}] {:name              (first name)
                                                           :token             full_token
                                                           :installed-version (clean-version (or version ""))})]
    (when (.exists metadata-dir)
      (let [path (-> metadata-dir (file-seq) (last) (.getPath))]
        (when (.endsWith path (str token ".json"))
          (-> (slurp path)
              (json/read-str :key-fn keyword)
              (make-cask)))))))

(defn read-all-casks-fs
  "Faster way to read casks info from the filesystem compared to `brew info --installed --json=v2`.
  There are a couple of caveats - some casks have their metadata in ruby files - need to parse Ruby,
  and there is no information about newer versions of the cask."
  []
  (let [cask-dirs (seq (.listFiles (jio/file caskroom)))]
    (vec (remove nil? (pmap read-cask-info cask-dirs)))))

(comment
  (time (read-all-casks-fs))

  ())
