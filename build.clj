(ns build
  (:require [clojure.tools.build.api :as b]))

(def main-class 'brew_updater.core)
(def class-dir "./target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file "./target/app.jar")
(def native-file "./target/app")

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

(defn native-image [_]
      "Generates native executable using GraalVM Native Image. Assumes uberjar already exists"
      (b/process {:command-args ["native-image"
                                 "--features=clj_easy.graal_build_time.InitClojureClasses"
                                 "--no-fallback"
                                 "-jar"
                                 uber-file
                                 native-file]}))
