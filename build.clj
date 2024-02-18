(ns build
  (:require [clojure.tools.build.api :as b]))

(def main-class 'brew_updater.core)
(def class-dir "./target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file "./target/app.jar")

(defn clean [_] (b/delete {:path "target"}))

(defn uber [_]
      (clean nil)
      (b/copy-dir {:src-dirs   ["src" "resources"]
                   :target-dir class-dir})
      (b/compile-clj {:basis        basis
                      :src-dirs     ["src"]
                      :class-dir    class-dir
                      :compile-opts {:direct-linking true
                                     :elide-meta     [:doc :file :line]}})
      (b/uber {:class-dir class-dir
               :uber-file uber-file
               :basis     basis
               :main      main-class}))

