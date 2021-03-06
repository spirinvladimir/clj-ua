(ns clj-ua.core
  (:require [clojure.contrib.command-line :as ccl]
            [clojure.string :as string]))

(import java.util.Arrays)

(defrecord Browser [name version])
(defrecord Platform [name version])
(defrecord UserAgent [#^Browser browser
                      #^Platform platform])

(defn- log [data]
  (prn (merge {:ns "clj-ua"} data)))

(defn- get-user-agent [browser platform]
  (UserAgent.
    (Browser. (:name browser) (:version browser))
    (Platform. (:name platform) (:version platform))))

(defn- parse-version-from-comment [comment]
  (let [version (re-find #"([0-9]+)[[_.]([A-Za-z0-9]+)]+" comment)]
    (if (nil? version)
      ; then
      (let [os-x (re-find #"OS X" (.toUpperCase comment))]
        (if (not (nil? os-x))
          os-x))
      ; else
      (.replaceAll (nth version 0) "_" "."))))

(defn- handle-comment [parts]
  (if (and (not (nil? parts)) (< 0 (alength parts)))
    (let [part (nth parts 0)]
      (if (< 1 (.length part))
        (apply array-map [:name part :version (parse-version-from-comment part)])
        (handle-comment (Arrays/copyOfRange parts 1 (count parts)))))))

(defn- parse-browser [seq]
  (let [part (last seq)]
    (if (or (nil? part) (.equals (nth part 0) ""))
      (parse-browser (subvec seq 0 (- (count seq) 1))) ;subvec is exclusive of the end
      (apply array-map [:name (nth part 1) :version (nth part 3)]))))

(defn- parse-platform [seq]
  (let [part (first seq)]
    (if (< 6 (count part))
      ; then
      (let [comment (nth part 6)]
        (if (not (nil? comment))
          (let [parts (.split comment "; ")]
            (if (< 1 (alength parts))
              (handle-comment (Arrays/copyOfRange parts 1 (count parts)))
              (handle-comment parts)))))
      ; else
      (parse-platform (rest seq)))))

(defn parse [#^String agent]
  (log {:fn "parse" :agent agent})
  (if (and (not (nil? agent)) (not (.equals agent "")))
    (let [seq (vec (re-seq #"([^/\s]*)(/([^\s]*))?(\s*\[[a-zA-Z][a-zA-Z]\])?\s*(\((([^()]|(\([^()]*\)))*)\))?\s*" agent))
          browser (parse-browser seq)
          platform (parse-platform seq)]
      (get-user-agent browser platform))
    (get-user-agent {:name "unknown" :version "unknown"} {:name "unknown" :version "unknown"})))

(defn -main [& args]
  (if args
    (let [agent (nth args 0)]
      (parse agent))
    (prn "Please pass a user-agent to parse.")))
