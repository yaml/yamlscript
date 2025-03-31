(def name "Juanita")

(def game ["Archery"
           "Boxing"
           "Chess"
           "Darts"])

(say (str "Meet, " name "."))

(+++
  (say
    (str "\n" name " plays "
         (__ game '(count))
         " games.\n\nHer favorite game is "
         (rand-nth game) "!" "\n")))
