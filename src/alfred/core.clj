(ns alfred.core
  (:require 
    [clojure.core.async :refer [<!!]]
    [clj-http.client :as http]
    [clojure.string :as str]
    [environ.core :refer [env]]
    [morse.handlers :as h]
    [morse.polling :as p]
    [morse.api :as t])
  (:gen-class))


(def token (env :telegram-token))
(def chat-ids (env :chat-ids))
(def link
  "https://www.aeroflot.ru/sb/app/ru-ru#/search?adults=2&cabin=econom&children=0&infants=0&routes=MOW.20181228.YKS")


(defn post! []
  (let [url    "https://www.aeroflot.ru/sb/booking/api/app/search/v4"
        params { :adults  2,
                 :cabin   "econom",
                 :country "ru",
                 :lang    "ru",
                 :routes [
                   { :departure_date "2018-12-28",
                     :destination    "YKS",
                     :origin         "MOW" }]}]
    (try
      (:body
        (http/post url
          { :as            :json
            :content-type  :json
            :cookie-policy :standard
            :form-params   params }))
      (catch Exception e
        (println "Aeroflot request failed:" url (pr-str params))
        (throw e)))))


(defn parseAeroflotData [data]
  (let [firstRoute     (first data)
        firstPrice     (first (:prices firstRoute))
        bestTotalPrice (:total_amount firstPrice)]
    (str "Билеты из Москвы в Якутск 28 декабря 2018 года от " bestTotalPrice " ₽" "\n" link)))

(defn getAeroflot []
  (let [body    (post!)
        data    (:data    body)
        success (:success body)]
    (cond
      (false? success)                     "Ошибка в запросе"
      (seq (first (:itineraries data)))    (parseAeroflotData (first (:itineraries data)))
      (empty? (first (:itineraries data))) "Нет билетов из Москвы в Якутск 28 декабря 2018 года"
      :else                                "Что-то пошло не так")))


(h/defhandler handler

  (h/command-fn "aeroflot"
    (fn [{{id :id :as chat} :chat}]
      (when (some #(= (str id) %) (str/split chat-ids #","))
        (println "Bot joined new chat: " chat)
        (t/send-text token id (str (getAeroflot))))))

  (h/message-fn
    (fn [{{id :id} :chat :as message}]
      (println "Intercepted message: " message)
      (t/send-text token id "Я вас не понимаю..."))))


(defn -main [& args]
  (when (str/blank? token)
    (println "Please provide token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the alfred")
  (<!! (p/start token handler)))

(comment
  (-main)
)
