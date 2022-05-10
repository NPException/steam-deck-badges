(ns steam-deck-badges.core
  (:require [org.httpkit.server :as server]
            [reitit.ring :as ring]
            [ring.middleware.params :as mw-params]
            [steam-deck-badges.badge :as badge])
  (:gen-class))

(defn ^:private compatibility-badge
  [{:keys [path-params query-params] :as _request}]
  (let [app-id (:app-id path-params)
        size   (parse-long (get query-params "size" ""))]
    (if-let [badge-bytes (-> (parse-long app-id)
                             (badge/badge-for-app size))]
      {:status  200
       :headers {"Content-Type"  "image/png"
                 "Cache-Control" (str "public, immutable, max-age=" badge/cache-duration-seconds)}
       :body    badge-bytes}
      {:status 404})))

(def ^:private router
  (ring/router
    [[["/compatibility-badge/{app-id}.png" {:name :route/compatibility-badge
                                            :get  {:handler    compatibility-badge}}]]]))

(def ^:private ring-handler
  (ring/ring-handler
    router
    (ring/create-default-handler)))

(defn ^:private print-request [req]
  (print "Request: ")
  (prn (select-keys req [:server-name :server-port :uri :query-string :scheme :request-method])))

(defn ^:private wrap-log-request
  [handler]
  (fn
    ([request]
     (print-request request)
     (handler request))
    ([request respond raise]
     (print-request request)
     (handler request respond raise))))

(defn -main
  "starts the server on a given :port (default 8080)"
  [& {:strs [port debug]
      :or   {port "8080"
             debug "true"}
      :as _args}]
  (server/run-server
    ((-> mw-params/wrap-params
         (cond-> (parse-boolean debug) (comp wrap-log-request)))
     #'ring-handler)
    {:port (Integer/parseInt port)})
  (println "Started server at port" port))
