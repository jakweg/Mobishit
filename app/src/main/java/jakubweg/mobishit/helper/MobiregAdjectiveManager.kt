package jakubweg.mobishit.helper

import java.util.*

object MobiregAdjectiveManager {
    // adjectives from https://synonim.net/synonim/beznadziejny
    private val adjectives = arrayOf(
            "beznadziejny", "brzydki", "fatalny", "karygodny", "koszmarny", "makabryczny", "nieładny", "niepiękny", "niżej wszelkiej krytyki",
            "obmierzły", "obrzydliwy", "oburzający", "odpychający", "odrażający", "odstręczający", "ohydny", "okropny", "opłakany", "paskudny",
            "poniżej wszelkiej krytyki", "porażający", "przeklęty", "przeokropny", "przerażający", "rażący", "skandaliczny", "szkaradny",
            "szpetny", "tragiczny", "wstrętny", "dramatyczny", "horrendalny", "katastrofalny", "niedobry", "niefortunny", "nieludzki",
            "niepomyślny", "nieszczęśliwy", "niewesoły", "pechowy", "przykry", "rozpaczliwy", "smutny", "szokujący", "wstrząsający",
            "zatrważający", "zgubny", "żałosny", "chałowy", "denny", "do bani", "do chrzanu", "do kitu", "do luftu", "do niczego",
            "dziadowski", "kiepski", "kijowy", "kulawy", "lichy", "lipny", "marny", "mierny", "mizerny", "nędzny", "nieklawy", "nijaki",
            "obciachowy", "od siedmiu boleści", "podły", "słaby", "syfiasty", "zakichany", "zasmarkany", "niemożliwy", "nieopisany", "niesłychany",
            "niespotykany", "zupełny", "diabelski", "niemiłosierny", "nieprzytomny", "niezmierny", "obłędny", "okrutny", "paniczny", "pioruński",
            "infernalny", "nieprzeciętny", "niezrównany", "ponadludzki", "przepotężny", "tęgi", "tytaniczny", "wściekły", "absurdalny", "bez głowy",
            "bez wyobraźni", "bezmózgi", "bezmózgowy", "bezmyślny", "bezrozumny", "bezsensowny", "bzdurny", "cepowaty", "ciemny", "ciemny jak tabaka w rogu",
            "durnowaty", "durny", "gamoniowaty", "gapiowaty", "gapiowski", "gapowaty", "głąbowaty", "głupi", "głupkowaty", "kanciasty", "kretyński",
            "mało inteligentny", "matołowaty", "mądry inaczej", "młotowaty", "niedorzeczny", "nieinteligentny", "nielogiczny", "nielotny", "niemądry",
            "nieobyty", "nieokrzesany", "niepojętny", "nieracjonalny", "nierozgarnięty", "nierozsądny", "nieroztropny", "nierozumny", "nonsensowny",
            "ograniczony", "pozbawiony sensu", "prymitywny", "przygłupi", "rozkojarzony", "roztargniony", "roztrzepany", "tępy", "tępy jak noga stołowa",
            "tumanowaty", "zwariowany", "haniebny", "krytyczny", "zły", "godny pożałowania", "godny ubolewania", "pożal się Boże", "smętny", "brutalny",
            "dantejski", "impasowy", "nie rokujący nadziei", "patowy", "przedagonalny", "stracony", "bezdenny", "bezkresny", "bezmierny", "nieogarniony",
            "bezwartościowy", "pod psem", "poniżej krytyki", "tandetny", "apokaliptyczny", "defetystyczny", "fatalistyczny", "kapitulancki", "kasandryczny",
            "katastroficzny", "pesymistyczny", "złowróżbny", "nieprzyjemny", "przygnębiający", "chałowaty", "pseudoartystyczny", "ramotowaty", "szmirowaty",
            "banalny", "barani", "bez sensu", "błazeński", "chory", "cudaczny", "dziwaczny", "głupawy", "idiotyczny", "irracjonalny", "niefajny", "nierozważny",
            "nieudany", "ośmieszający", "pokręcony", "poplątany", "poroniony", "przygłupiasty", "walnięty", "badziewny", "bublowaty", "jarmarczny", "kiczowaty",
            "nieodpowiedni", "bardzo zły", "byle jaki", "w złym gatunku", "wykonany niedbale", "nie najlepszy", "niedoskonały", "stagnacyjny", "posępny",
            "złowieszczy", "ciężki", "poważny", "niestarannie wykonany", "na wariackich papierach", "partacki", "sknocony", "bardzo słaby", "cienki", "nieszczególny",
            "desperacki", "nieszczęsny", "bezowocowy", "daremny", "jałowy")

    private val random = Random()

    fun getRandom() = adjectives[random.nextInt(adjectives.size)]
}