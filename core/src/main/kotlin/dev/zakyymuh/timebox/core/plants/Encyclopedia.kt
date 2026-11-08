package dev.zakyymuh.timebox.core.plants

import java.time.LocalDate

/** A named form of a plant: a variety, a cultivar or a variegated form. */
data class Variety(val name: String, val note: String)

/** One encyclopedia entry. Plain text, bundled with the app — it has no internet access. */
data class PlantInfo(
    val id: String,
    val name: String,
    val otherNames: String,
    val origin: String,
    /** One line, for the list. */
    val summary: String,
    val description: String,
    val sun: String,
    val water: String,
    val tips: List<String>,
    val varieties: List<Variety>,
    val funFacts: List<String>,
    val sources: List<String>,
    /** Lower-case words that identify this plant inside a user's plant name, most specific first. */
    val keywords: List<String>,
)

/**
 * The plants the owner keeps, written from nursery care guides, botanical references and
 * growers' forums (named at the bottom of each entry). Where sources disagree — mostly on how
 * much sun — the entry gives the cautious middle and says so.
 */
object Encyclopedia {

    private val watering = PlantInfo(
        id = "watering-basics",
        name = "Watering basics",
        otherNames = "Soak and dry — for every cactus and succulent here",
        origin = "—",
        summary = "Water deeply, then wait until the soil is completely dry.",
        description = "Cacti and succulents store water in their leaves and stems. They die far more often " +
            "from too much water than too little: wet soil rots the roots, and by the time the plant looks " +
            "unwell the damage is done. The rule is the same for all of them: soak, drain, then wait.",
        sun = "Most want bright light. Pale, stretched growth means too little; beige or brown patches " +
            "mean sunburn — move it gradually rather than all at once.",
        water = "Soak until water runs out of the drainage hole, then wait until the soil is completely dry " +
            "before watering again. Push a finger or a wooden stick 2–3 cm into the soil: if it comes out " +
            "damp, wait. Intervals in this encyclopedia are only a rough guide — the soil decides.",
        tips = listOf(
            "Always use a pot with a drainage hole, and never leave it standing in a saucer of water.",
            "A gritty, mineral mix (pumice, perlite, coarse sand) dries fast; plain garden soil stays wet too long.",
            "Terracotta pots breathe and dry faster than plastic.",
            "In Jakarta's steady heat and humidity, soil can stay damp longer than you expect indoors — check before you water.",
            "When in doubt, wait a few days. A thirsty succulent recovers; a rotten one rarely does.",
        ),
        varieties = emptyList(),
        funFacts = listOf(
            "Most succulents that die indoors die from too much water, not too little.",
            "A slightly soft, wrinkled cactus is usually just thirsty — it plumps up again within days of watering.",
            "The 'Still moist' button exists because checking the soil beats any calendar.",
            "Terracotta pots let water evaporate through their walls, so the soil dries faster than in plastic.",
        ),
        sources = listOf("Mountain Crest Gardens care guides", "World of Succulents", "Gardening Know How"),
        keywords = emptyList(),
    )

    private val greenovia = PlantInfo(
        id = "greenovia",
        name = "Greenovia",
        otherNames = "Mountain Rose · Greenovia dodrentalis / Aeonium dodrantale",
        origin = "Canary Islands",
        summary = "A rosette that looks like a rose bud, and closes up in the heat.",
        description = "Tight rosettes of blue-green leaves that look like small rose buds, growing in clumps. " +
            "In hot weather it goes semi-dormant: the rosette closes tightly to save water and some outer " +
            "leaves dry up, then it opens again when it is cooler. It is now often sold under Aeonium, " +
            "because botanists disagree about whether Greenovia is its own genus.",
        sun = "Bright light for about 4–6 hours a day, with shade from the hottest midday and afternoon sun. " +
            "Sources disagree (some say full sun, some bright indirect light); in a hot climate, bright shade " +
            "or gentle morning sun is the safer choice.",
        water = "Soak, then wait until the soil is completely dry. While it is open and growing (cooler weather), " +
            "roughly every 1–2 weeks. When the rosette closes up in heat, water much less — about every 3–4 " +
            "weeks, or even less. A closed rosette means 'less water', not 'more'. In Jakarta's year-round " +
            "heat, keep it on the dry side.",
        tips = listOf(
            "Use a gritty, fast-draining mix; a low clump can hold moisture around its roots for a long time.",
            "Water the soil, not into the rosette.",
            "Dry outer leaves in hot weather are normal: the plant gives them up to protect its centre.",
            "Protect it from frost; it is only hardy in warm climates.",
        ),
        varieties = listOf(
            Variety("Greenovia dodrentalis (Mountain Rose)", "Small rosettes that form clumps; the classic 'rose'."),
            Variety("Greenovia aurea", "Larger, golden-green to blue rosettes; endemic to the central and western Canary Islands."),
            Variety("Greenovia diplocycla", "The only Greenovia on La Palma; a 'Gigantea' form makes single rosettes up to 46 cm wide."),
        ),
        funFacts = listOf(
            "In hot weather Greenovia closes into a tight bud to save water, then opens again when it cools down.",
            "Every wild Greenovia comes from the Canary Islands.",
            "Botanists still argue whether Greenovia is its own genus or part of Aeonium — that's why it has two names.",
            "A 'Gigantea' form of Greenovia diplocycla can grow a single rosette 46 cm wide.",
            "When Greenovia's outer leaves dry up in summer, it is sacrificing them to protect the heart of the rosette.",
        ),
        sources = listOf(
            "Mountain Crest Gardens — Aeonium dodrantale",
            "Gardening Know How — Greenovia dodrentalis",
            "World of Succulents — Aeonium dodrantale",
            "Huntington Botanical Gardens (ISI 2025)",
            "Botánica Macaronésica 31 (2021)",
        ),
        keywords = listOf("greenovia", "mountain rose", "dodrantal", "dodrental"),
    )

    private val asterias = PlantInfo(
        id = "astrophytum-asterias",
        name = "Astrophytum asterias",
        otherNames = "Sea urchin cactus · Star cactus · Kabuto (兜丸)",
        origin = "Starr County, Texas, and Nuevo León & Tamaulipas, Mexico",
        summary = "A flat, spineless dome dotted with white wool — like a sea urchin.",
        description = "A small, flat, spineless cactus divided into about eight segments, each lined with " +
            "white woolly tufts and usually dusted with white flecks. Yellow flowers with red centres " +
            "open on top. It grows slowly and is endangered in the wild; the ones sold are nursery-grown.",
        sun = "Strong light most of the day, but shade from the harshest midday and afternoon sun — " +
            "especially young plants. Growers disagree between full sun and bright filtered light; " +
            "filtered light is the safer choice in a hot climate.",
        water = "Only when the soil is completely dry — roughly every 2–3 weeks in warm, growing weather, " +
            "when the body feels very slightly softer. Little or no water in the cool, resting season. " +
            "It rots easily, so err on the dry side.",
        tips = listOf(
            "Gritty, mineral, very fast-draining soil; it hates wet feet.",
            "Rot is the main risk, especially in cool or dark periods — that's when to water least.",
            "Turn the pot now and then so it doesn't lean toward the light.",
        ),
        varieties = listOf(
            Variety("'Super Kabuto'", "A Japanese cultivar with extremely dense white flecking."),
            Variety("var. nudum", "Little or no white flecking — a plainer, greener dome."),
            Variety(
                "f. variegata (variegated)",
                "Yellow or cream patches with little or no chlorophyll. Burns more easily: give bright shade with " +
                    "only gentle morning sun. Grows slower; heavily variegated plants are often grafted onto a " +
                    "green rootstock because they can't feed themselves. Beige patches mean sunburn. " +
                    "Water the same way, even more carefully.",
            ),
        ),
        funFacts = listOf(
            "Astrophytum asterias is called the sea urchin cactus because it looks like a sea urchin without spines.",
            "In the wild, the sea urchin cactus is endangered: protected in the US and listed on CITES Appendix I.",
            "In Japanese it's Kabuto-maru — 'kabuto' is a samurai helmet. 'Super Kabuto' is a Japanese cultivar with extra white flecks.",
            "A sea urchin cactus can take up to 6 years to flower for the first time.",
            "Wild sea urchin cacti survive only in a small part of Starr County, Texas, and a few sites in Tamaulipas, Mexico.",
            "Variegated cacti are often grafted onto a green cactus because their yellow parts can't make enough food.",
        ),
        sources = listOf(
            "Wikipedia — Astrophytum asterias (US Fish and Wildlife Service, CITES)",
            "LLIFLE — Astrophytum asterias",
            "Cactiguide.com growers' forum",
            "Secret Garden nursery — Astrophytum asterias",
            "World of Succulents — grafted cacti",
        ),
        keywords = listOf("asterias", "kabuto", "sea urchin"),
    )

    private val myriostigma = PlantInfo(
        id = "astrophytum-myriostigma",
        name = "Astrophytum myriostigma",
        otherNames = "Bishop's cap · Bishop's mitre · Star cactus",
        origin = "Mexico",
        summary = "A spineless star of five ribs, speckled with countless white dots.",
        description = "A spineless cactus with usually five sharp ribs, so it looks like a star from above — " +
            "or a bishop's mitre from the side. The body is covered in tiny white flecks, made of fine hairs. " +
            "Yellow flowers appear on top in warm weather.",
        sun = "Full sun, ideally 6–8 hours a day; move it into stronger light gradually. Indoors, the brightest " +
            "window or a grow light. Variegated forms need softer light (see below).",
        water = "Soak and dry — roughly every 3 weeks in the growing season, always waiting until the soil is " +
            "completely dry. No water in the cool, resting period.",
        tips = listOf(
            "Letting the soil dry out completely between waterings is what prevents root rot.",
            "Gritty, mineral soil and a pot with a drainage hole.",
            "Only hardy in warm climates; keep it frost-free.",
        ),
        varieties = listOf(
            Variety("var. nudum", "Smooth, with no white dots, and greener than the flecked form."),
            Variety("var. quadricostatum", "Four ribs instead of five, so it looks square from above."),
            Variety("tricostatum", "Three ribs — a triangular form."),
            Variety("'Onzuka'", "A Japanese cultivar with dense, patterned white flecking."),
            Variety(
                "f. variegata (variegated)",
                "Yellow or cream patches without chlorophyll; often grafted onto a column cactus. Give bright " +
                    "shade with gentle morning sun instead of full sun, and water even more carefully.",
            ),
        ),
        funFacts = listOf(
            "'Myriostigma' means 'countless dots' — the white flecks on a bishop's cap are tiny hairs.",
            "From above, a bishop's cap looks like a star; from the side, like a bishop's mitre — hence the name.",
            "Astrophytum myriostigma var. nudum has no white dots at all, and is greener than the classic form.",
            "Most bishop's caps have five ribs, but there are three-, four- and many-ribbed forms.",
            "Variegated bishop's caps are often grafted onto a column cactus, because their yellow parts can't feed the plant.",
        ),
        sources = listOf(
            "World of Succulents — Bishop's cap",
            "Mountain Crest Gardens — Astrophytum myriostigma",
            "Succulents and Sunshine — Astrophytum myriostigma",
            "Gardener's Path — Astrophytum myriostigma",
            "Cactus-art.biz — Astrophytum myriostigma variegata",
        ),
        keywords = listOf("myriostigma", "myrio", "bishop"),
    )

    /**
     * Most specific first, because [matching] takes the first hit: "asterias kikko" must find
     * Kikko, "Astrophytum myriostigma" must not match asterias. Guides go last.
     */
    val entries: List<PlantInfo> = with(EncyclopediaPlants) {
        listOf(
            kikko, greenovia, asterias, myriostigma, gymnoLbVos, gymnoMiha, sansevieria, echeveria,
            pseudolithos, coryphantha, mammillaria, koboi,
        )
    } + with(EncyclopediaGuides) {
        listOf(cactiAndSucculents, grafting, graftingDragonFruit, soilMix, seedSowing)
    } + watering

    fun byId(id: String): PlantInfo? = entries.firstOrNull { it.id == id }

    /** The entry a user's plant name refers to, if any ("Greenovia", "my bishop's cap"…). */
    fun matching(plantName: String): PlantInfo? {
        val name = plantName.lowercase()
        return entries.firstOrNull { entry -> entry.keywords.any { it in name } }
    }
}

data class FunFact(val text: String, val entry: PlantInfo)

/** The dashboard's "Did you know?": one fact per day, the same all day. */
object FunFacts {

    /** Any fact from the whole encyclopedia; the dashboard draws a new one each time the app opens. */
    fun random(random: kotlin.random.Random): FunFact =
        Encyclopedia.entries.flatMap { entry -> entry.funFacts.map { FunFact(it, entry) } }.random(random)

    /** Facts about plants the owner actually has come first; otherwise every entry takes a turn. */
    fun forDay(date: LocalDate, ownedPlantNames: List<String>): FunFact {
        val owned = ownedPlantNames.mapNotNull(Encyclopedia::matching).distinct()
        val pool = (owned.ifEmpty { Encyclopedia.entries })
            .flatMap { entry -> entry.funFacts.map { FunFact(it, entry) } }
        return pool[Math.floorMod(date.toEpochDay(), pool.size.toLong()).toInt()]
    }
}
