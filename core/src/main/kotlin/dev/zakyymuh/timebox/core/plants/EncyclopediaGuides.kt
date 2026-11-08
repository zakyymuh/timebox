package dev.zakyymuh.timebox.core.plants

/**
 * How-to entries: no single plant, so no keywords. Written from growers' guides (Cactiguide
 * forum, LLIFLE, World of Succulents, Mountain Crest Gardens, cactus-art.biz) and adapted to
 * materials sold in Indonesia (pasir malang, sekam bakar, zeolite).
 */
internal object EncyclopediaGuides {

    val cactiAndSucculents = PlantInfo(
        id = "cacti-succulents",
        name = "Cacti & succulents",
        otherNames = "Kaktus & sukulen — what they are and how they cope",
        origin = "Succulents: dry places worldwide · Cacti: the Americas",
        summary = "Every cactus is a succulent; not every succulent is a cactus.",
        description = "A succulent is any plant that stores water in thick leaves, stems or roots — a survival " +
            "strategy that evolved separately in dozens of plant families: aloes, echeverias, euphorbias, " +
            "stapeliads like Pseudolithos, and more. Cacti are one family (Cactaceae) of about 1,800 species. " +
            "What makes a cactus a cactus is the areole: a small woolly cushion that grows spines, flowers and new " +
            "shoots. Euphorbias can look exactly like cacti but have no areoles and bleed milky sap.",
        sun = "Most want bright light and some direct sun. Pale, stretched growth means too little light; " +
            "beige or brown scars mean sunburn. Change their light gradually, over a week or two.",
        water = "Soak, drain, then wait until the soil is fully dry. See 'Watering basics' — the soil decides, " +
            "not the calendar.",
        tips = listOf(
            "Spines, wool and powder are the plant's sun protection: don't scrub them off.",
            "Airflow matters as much as water in humid climates; a breezy spot prevents rot and fungus.",
            "Quarantine new plants for a couple of weeks and check for mealybugs before they join the others.",
            "Most grow in the warm, wet season and slow down when it is cooler or duller — water to match.",
        ),
        varieties = emptyList(),
        funFacts = listOf(
            "Every cactus is a succulent, but most succulents are not cacti.",
            "Cactus spines are leaves that evolved into needles — they shade the plant and stop thirsty animals.",
            "Areoles, the woolly dots that grow spines and flowers, are found only on cacti.",
            "Almost all cacti come from the Americas; the one exception, Rhipsalis baccifera, also grows wild in Africa and Sri Lanka.",
            "Cacti and many succulents breathe at night (CAM photosynthesis) to lose less water in the heat of the day.",
            "A big saguaro can hold more than a thousand litres of water after a good rain.",
            "Succulence evolved separately in dozens of plant families — nature invented it again and again.",
            "Some cacti live for 150 years or more.",
            "The ribs of a cactus work like an accordion: they expand when it drinks and fold when it's dry.",
            "Cactus flowers often last just a day or two; some open only at night.",
        ),
        sources = listOf("Wikipedia — Cactus; Succulent plant; Rhipsalis baccifera", "LLIFLE — Cactaceae", "US National Park Service — Saguaro"),
        keywords = emptyList(),
    )

    val grafting = PlantInfo(
        id = "grafting",
        name = "Grafting",
        otherNames = "Sambung kaktus — tips and techniques",
        origin = "—",
        summary = "Joining a cactus top onto a stronger rootstock so it grows faster or survives at all.",
        description = "Grafting joins a 'scion' (the plant you want) onto a 'stock' (a vigorous cactus with " +
            "roots). The two grow together through their vascular ring — the pale circle you see in a cut cactus. " +
            "Growers graft to speed up slow plants, to keep chlorophyll-free forms like red moon cacti alive, to " +
            "rescue a plant whose roots rotted, or to save a precious top. Common stocks: dragon fruit " +
            "(Hylocereus), Pereskiopsis for tiny seedlings, Echinopsis / Trichocereus and Myrtillocactus.",
        sun = "After grafting: bright shade, out of direct sun and rain, for 1–2 weeks. Then back to normal " +
            "light gradually.",
        water = "Keep water off the cut. Water the stock's soil normally after about 3–5 days if it is dry; a " +
            "plump, well-watered stock takes grafts best, so water it a few days before you graft.",
        tips = listOf(
            "Graft in warm, growing weather; both plants should be firm and well hydrated.",
            "Use a very sharp, thin blade wiped with alcohol between every cut.",
            "Flat graft (most common): cut the stock flat, bevel its edges, cut the scion flat, and press them together at once.",
            "Overlap the vascular rings: if the scion is smaller, set it off-centre so its ring crosses the stock's ring.",
            "Hold the graft with rubber bands or string over the pot, firm but not crushing, for 1–2 weeks.",
            "Cleft graft for flat or thin stems (like Schlumbergera): split the stock top and slide in a wedge-cut scion; pin it with a spine.",
            "Seedling grafting: 2–8 week-old seedlings onto Pereskiopsis can grow years' worth in months.",
            "The graft has taken when the scion stays plump and starts growing; a scion that shrivels or slides off has failed.",
            "Remove shoots from the stock, and re-root or regraft once the stock starts to tire.",
        ),
        varieties = listOf(
            Variety("Flat graft", "For round cacti onto columnar stock. Quick and reliable."),
            Variety("Cleft / wedge graft", "For flat or thin-stemmed scions."),
            Variety("Side graft", "For thin columnar scions: both cut on a long slant and bound side by side."),
            Variety("Pereskiopsis seedling graft", "Tiny seedlings onto a leafy, fast-growing stock for speed."),
        ),
        funFacts = listOf(
            "Red and yellow moon cacti can only live grafted: they have no chlorophyll to feed themselves.",
            "A seedling grafted onto Pereskiopsis can grow in months what would take years on its own roots.",
            "In a successful graft, the scion and stock join through the pale ring you see when you cut a cactus.",
            "Grafting can rescue a cactus whose roots have rotted: cut above the rot and graft the healthy top.",
        ),
        sources = listOf("Cactiguide.com growers' forum — grafting", "LLIFLE — Grafting cacti", "World of Succulents — grafted cacti"),
        keywords = emptyList(),
    )

    val graftingDragonFruit = PlantInfo(
        id = "grafting-dragon-fruit",
        name = "Grafting onto dragon fruit",
        otherNames = "Sambung di batang bawah buah naga (Hylocereus / Selenicereus)",
        origin = "—",
        summary = "The fast, cheap rootstock behind most moon cacti and grafted gymnos in Indonesia.",
        description = "Dragon fruit (buah naga, Hylocereus or Selenicereus) is a climbing jungle cactus that grows " +
            "fast, roots easily and is cheap everywhere in Indonesia, so it is the favourite stock for moon cacti, " +
            "Gymno LB, variegated Astrophytum and seedlings. It pushes growth hard. The trade-offs: it dislikes " +
            "cold (below about 10 °C), wants more water and richer soil than desert cacti, and often gives out " +
            "after a few years, so grafted plants are usually regrafted or rooted later.",
        sun = "Bright light, shaded from harsh midday sun while the graft heals and while the stock is young. " +
            "Dragon fruit burns pale or yellow in sudden full sun.",
        water = "More than most cacti: water when the top of the soil is dry, about every 5–10 days in warm " +
            "weather, but never leave it soggy. A plump stock grows the scion faster.",
        tips = listOf(
            "Root dragon fruit cuttings first, and graft once they are rooted and firm, not freshly cut.",
            "Choose a young, green, firm segment about as thick as the scion; avoid woody, brown parts.",
            "Cut the stock flat 2–5 cm above the soil or above a joint; its three-winged stem has the vascular ring in the centre.",
            "Bevel the three wings so they don't push the scion off as they dry.",
            "Seat the scion so its ring overlaps the stock's centre ring, then bind with rubber bands for 1–2 weeks.",
            "Rub off every side shoot the stock tries to make.",
            "Use a richer mix than for desert cacti — about half organic (cocopeat, compost, sekam bakar) and half grit.",
        ),
        varieties = emptyList(),
        funFacts = listOf(
            "The same plant that gives you buah naga fruit carries most of the moon cacti sold in Indonesia.",
            "Dragon fruit is a climbing jungle cactus that clings to trees with aerial roots.",
            "Dragon fruit flowers open at night and are called 'queen of the night' — they're over by morning.",
            "A dragon fruit stock can push a grafted gymno to flowering size years sooner than its own roots.",
        ),
        sources = listOf("World of Succulents — Hylocereus as rootstock", "Cactiguide.com growers' forum — grafting on Hylocereus", "Wikipedia — Pitaya"),
        keywords = emptyList(),
    )

    val soilMix = PlantInfo(
        id = "soil-mix",
        name = "Soil mix",
        otherNames = "Media tanam kaktus & sukulen",
        origin = "—",
        summary = "Mostly grit, a little organic: a mix that drains in seconds and dries in days.",
        description = "Cacti and succulents need a mix that lets water through at once and lets air reach the " +
            "roots. Plain garden soil or potting soil stays wet for too long and rots them. A good mix is mostly " +
            "mineral grit with a smaller part of organic matter; the wetter your climate and the more delicate " +
            "the plant, the more grit. Materials easy to find in Indonesia work well.",
        sun = "Not a plant — but the brighter and hotter the spot, the faster any mix dries and the more organic " +
            "matter you can afford.",
        water = "A good mix drains as fast as you pour. If water sits on top or the pot is still heavy a week " +
            "later, the mix holds too much water: add grit.",
        tips = listOf(
            "Everyday mix: about 60% grit (pasir malang, pumice/batu apung, zeolite) and 40% organic (sekam bakar, cocopeat, compost).",
            "Delicate plants (Astrophytum, Pseudolithos, Ariocarpus): 80–100% mineral, with just a little organic.",
            "Leafy or jungle cacti (dragon fruit stock, Rhipsalis) and Sansevieria: about half organic.",
            "Sift out dust and rinse pasir malang: the fines clog the mix and keep it wet.",
            "Sekam bakar (burnt rice husk) is light and airy and slightly alkaline; cocopeat holds water — use it sparingly.",
            "A top layer of gravel keeps the plant's base dry and stops the mix splashing.",
            "Akadama and kanuma are good but break down into mud after a year or two; repot or refresh then.",
        ),
        varieties = listOf(
            Variety("Pasir malang", "Volcanic sand from East Java: heavy, gritty, cheap. Rinse before use."),
            Variety("Pumice (batu apung)", "Light, porous; holds a little water and lots of air."),
            Variety("Zeolite", "Porous mineral that holds nutrients; adds weight and stability."),
            Variety("Sekam bakar", "Burnt rice husk: airy organic matter that doesn't pack down."),
            Variety("Cocopeat / compost", "Holds water and food; keep it a small part of the mix."),
        ),
        funFacts = listOf(
            "Plain potting soil kills more cacti than any pest: it stays wet for too long.",
            "Pasir malang is volcanic sand — your cactus grows in a little bit of an East Java volcano.",
            "Roots need air as much as water; a gritty mix lets them breathe.",
            "In very humid weather, the right mix matters even more than how often you water.",
        ),
        sources = listOf("Mountain Crest Gardens — soil for succulents", "World of Succulents — potting mix", "Cactiguide.com growers' forum"),
        keywords = emptyList(),
    )

    val seedSowing = PlantInfo(
        id = "seed-sowing",
        name = "Seed sowing",
        otherNames = "Semai biji kaktus & sukulen",
        origin = "—",
        summary = "Fine mix, seeds on the surface, a clear lid, warmth and bright shade.",
        description = "Growing cacti from seed is cheap, fun and the only way to get unusual forms like kikko " +
            "or patterned gymnos in quantity. Seedlings are tiny and soft for their first months and need " +
            "conditions very unlike adult cacti: steady moisture and high humidity, then slowly weaned to dry, " +
            "bright conditions.",
        sun = "Bright shade or under a white LED for 12–14 hours a day; never direct sun through the lid, which " +
            "cooks them. Give more light gradually as they grow.",
        water = "Water from below by standing the pot in water until the surface darkens. Keep the mix lightly " +
            "moist (never soggy) for the first 2–3 months, then let it dry a little between waterings.",
        tips = listOf(
            "Fresh seed germinates best; Astrophytum seed loses vigour within about a year.",
            "Mix: finely sifted grit (pumice, pasir malang) with a little sifted cocopeat; scald it with boiling water first to kill fungus.",
            "Sprinkle seeds on the surface and press lightly; don't bury small seeds — many need light to sprout.",
            "Cover with a clear lid or zip bag; 25–30 °C is ideal. Many cacti sprout in 3–14 days; some take weeks.",
            "Open the lid a little more each week after the first month so seedlings harden off.",
            "Damping off (seedlings collapsing at the base) is fungus: more air, less water, and a fungicide if it spreads.",
            "Prick out into community pots after 6–12 months; grafting onto Pereskiopsis can speed them up enormously.",
            "To make your own seed, brush pollen between two different plants — Astrophytum won't set seed with its own pollen.",
        ),
        varieties = emptyList(),
        funFacts = listOf(
            "Astrophytum seeds can sprout in as little as two or three days.",
            "Many cactus seeds need light to germinate, so they're sown on the surface, not buried.",
            "A cactus seedling's first 'leaves' are two tiny cotyledons; the spines come next.",
            "Some cactus seedlings take ten years to reach flowering size on their own roots.",
        ),
        sources = listOf("Cactiguide.com growers' forum — sowing", "LLIFLE — Growing cacti from seed", "cactus-art.biz — seed sowing"),
        keywords = emptyList(),
    )
}
