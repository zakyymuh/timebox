package dev.zakyymuh.timebox.core.plants

/**
 * The second batch of plants, written from botanical references (Kew POWO, LLIFLE,
 * Wikipedia), nursery care guides (World of Succulents, Mountain Crest Gardens) and growers'
 * collections (cactus-art.biz, pukubook.jp). Where hobby names are loose — "Gymno LB",
 * "kaktus koboi" — the entry says what is known and what is only trade usage.
 */
internal object EncyclopediaPlants {

    val sansevieria = PlantInfo(
        id = "sansevieria",
        name = "Sansevieria",
        otherNames = "Lidah mertua · Snake plant · Mother-in-law's tongue · now Dracaena trifasciata",
        origin = "Tropical West Africa (Nigeria to Congo); other species across Africa, Madagascar and South Asia",
        summary = "Stiff, upright leaves that survive almost anything — except too much water.",
        description = "Sansevieria grows stiff, sword-like leaves from creeping underground stems (rhizomes), " +
            "often banded in dark and light green. In Indonesia it is 'lidah mertua' — mother-in-law's tongue — " +
            "a joke about leaves as long and sharp as a sharp-tongued mother-in-law; the English name is the same " +
            "joke. Since 2017 botanists class it inside Dracaena (the common one is Dracaena trifasciata), but " +
            "shops and growers still say sansevieria, or just 'sanse'. It is one of the easiest houseplants there is.",
        sun = "Very adaptable. Grows best and keeps its strongest colours in bright, indirect light or a few hours " +
            "of gentle sun; tolerates a dim corner, but grows very slowly there and should then be watered even " +
            "less. Move it into strong sun gradually or the leaves scorch.",
        water = "Only when the soil is completely dry: roughly every 2–3 weeks in bright, warm spots, every 4–6 " +
            "weeks in dim ones. Never let water sit in the centre of the leaf rosette. Soft, yellow, mushy leaves " +
            "at the base mean rot from overwatering — the most common way it dies.",
        tips = listOf(
            "Gritty, fast-draining mix, like a cactus mix with some cocopeat or compost; a heavy pot so tall plants don't tip.",
            "It likes to be a little root-bound; repot only when the rhizomes crack or bend the pot.",
            "Divide clumps at repotting: cut the rhizome so each piece has leaves and roots, and let cuts dry a day.",
            "Leaf cuttings root in water or soil, but a leaf cutting of a yellow-edged plant grows back plain green — divide those instead.",
            "Mildly toxic if chewed (saponins): keep it from cats and dogs that nibble.",
            "Wipe the leaves now and then; dust blocks light.",
        ),
        varieties = listOf(
            Variety("'Laurentii'", "The classic: tall banded leaves with bright yellow edges."),
            Variety("'Hahnii' (bird's nest)", "Dwarf, short leaves in a flat rosette, 10–15 cm tall. 'Golden Hahnii' has wide yellow stripes."),
            Variety("'Moonshine'", "Pale silvery green leaves with almost no banding; likes less direct sun."),
            Variety("Cylindrica (Dracaena angolensis)", "Round, pencil-like leaves, often sold braided or fanned."),
            Variety("Masoniana 'Whale Fin'", "One or two very wide, paddle-shaped leaves; slow and prized."),
            Variety("'Black Gold' / 'Black Coral'", "Very dark green leaves, with yellow edges (Black Gold) or silvery bands (Black Coral)."),
            Variety("Kirkii 'Coppertone'", "Wavy, coppery-brown leaves that colour up in bright light."),
            Variety("Ehrenbergii / 'Samurai'", "Leaves in a flat fan along one plane, thick and blue-green."),
            Variety("Ballyi and other dwarfs", "Miniature species and hybrids that stay under 10 cm — popular with collectors."),
        ),
        funFacts = listOf(
            "Sansevieria is named after Raimondo di Sangro, an 18th-century Italian prince of San Severo.",
            "'Lidah mertua' and 'mother-in-law's tongue' are the same joke in two languages: long, sharp leaves.",
            "Snake plants open their pores at night (CAM photosynthesis) to save water in the heat of the day.",
            "A snake plant was part of NASA's 1989 Clean Air Study — but cleaning a room's air would take far more plants than any home holds.",
            "Grow a leaf cutting of yellow-edged 'Laurentii' and the new plant comes back plain green.",
            "Botanists moved Sansevieria into Dracaena, the dragon-tree genus, in 2017.",
        ),
        sources = listOf(
            "Kew Plants of the World Online — Dracaena trifasciata",
            "Wikipedia — Dracaena trifasciata; Raimondo di Sangro",
            "Missouri Botanical Garden Plant Finder",
            "World of Succulents — Sansevieria",
            "NASA Clean Air Study (Wolverton et al., 1989)",
        ),
        keywords = listOf(
            "sansevieria", "sanseveria", "sanseviera", "sansiveira", "sansiviera", "sanse", "lidah mertua",
            "snake plant", "mother-in-law", "trifasciata", "whale fin", "masoniana", "hahnii", "laurentii", "cylindrica",
        ),
    )

    val pseudolithos = PlantInfo(
        id = "pseudolithos",
        name = "Pseudolithos",
        otherNames = "'False stone' · P. migiurtinus, P. cubiformis, P. dodsonianus",
        origin = "Somalia, and nearby Ethiopia, Kenya and Yemen",
        summary = "A succulent that looks like a pebble, with flowers that smell of carrion.",
        description = "Small, round or cube-shaped stems with a lumpy, pebbly skin in grey-green to brown — the " +
            "name means 'false stone', and in the wild they hide among real stones. Not a cactus but a stapeliad " +
            "(milkweed family, Apocynaceae). Tiny star-shaped flowers appear in clusters at the sides and smell " +
            "of rotting meat to attract the flies that pollinate them. Rare, slow and famously tricky.",
        sun = "Bright light with some gentle direct sun, but shade from harsh midday sun; very low light makes " +
            "them weak and rot-prone.",
        water = "Very sparingly. In warm weather, water only when the soil has been bone-dry for several days, and " +
            "only in the morning so it dries by night. Little to none when it is cool or overcast. Wet, cool " +
            "soil kills them quickly.",
        tips = listOf(
            "An almost fully mineral mix (pumice, pasir malang, zeolite) in a small pot; strong airflow.",
            "Many growers keep them grafted on a sturdier stapeliad (such as Stapelia or Orbea) because own-root plants rot so easily.",
            "Water around the edge of the pot, never on the plant body.",
            "Keep warm: they dislike anything below about 15 °C.",
        ),
        varieties = listOf(
            Variety("P. migiurtinus", "Round to flattened, covered in warty bumps; the one most often sold."),
            Variety("P. cubiformis", "Grows into a squarish, cube-like stem with a tiled surface."),
            Variety("P. dodsonianus", "Small, clumping, with a smoother surface."),
        ),
        funFacts = listOf(
            "Pseudolithos means 'false stone' — in the wild it hides among real pebbles.",
            "Pseudolithos flowers smell of rotting meat to attract the flies that pollinate them.",
            "Pseudolithos is not a cactus: it belongs to the milkweed family, like Stapelia and Hoya.",
            "Many Pseudolithos in collections are grafted, because on their own roots they rot so easily.",
        ),
        sources = listOf("LLIFLE — Pseudolithos", "World of Succulents — Pseudolithos migiurtinus", "Wikipedia — Pseudolithos"),
        keywords = listOf("pseudolithos", "pseudolitos", "migiurtinus", "cubiformis"),
    )

    val echeveria = PlantInfo(
        id = "echeveria",
        name = "Echeveria",
        otherNames = "Mexican hens-and-chicks · Painted lady · about 150 species and thousands of hybrids",
        origin = "Mexico to north-western South America, mostly mountains",
        summary = "The classic rose-shaped succulent, in every colour from blue to pink to black.",
        description = "Rosettes of fleshy leaves, often with a powdery coating (farina) that protects them from " +
            "sun. Bell-shaped flowers hang from long side stalks. Most species come from cool, bright mountains, " +
            "so in hot, humid lowlands they need strong light, airflow and careful watering; highland growers " +
            "(Lembang, Batu, Dieng) get the richest colours. Leaves wiped or handled lose their powder for good.",
        sun = "As much bright light as possible: 4–6 hours of direct sun, with some shade at the hottest midday " +
            "and afternoon. Too little light makes them stretch and lose colour (etiolation); stress from sun " +
            "and dryness brings out reds and pinks.",
        water = "Soak, then wait until completely dry — about every 7–10 days in hot, bright weather. Water the " +
            "soil, not the rosette: water left in the centre rots it. Wrinkled lower leaves mean thirsty; " +
            "translucent, mushy leaves mean too much.",
        tips = listOf(
            "Gritty mix with at least half mineral (pasir malang, pumice); a fan or breezy spot helps a lot in humid weather.",
            "Pull off dried lower leaves: they hide mealybugs and trap moisture.",
            "Propagate from a whole leaf twisted off cleanly; lay it on dry mix in bright shade and wait for roots.",
            "A stretched plant can be beheaded: cut the rosette, let it dry, and re-root it; the stump grows pups.",
        ),
        varieties = listOf(
            Variety("E. elegans (Mexican snowball)", "Pale blue-green, tight rosettes that clump."),
            Variety("'Perle von Nürnberg'", "Pinkish-lilac hybrid with a pearly coating; a classic."),
            Variety("E. lilacina (ghost echeveria)", "Silvery-white, very powdery leaves."),
            Variety("E. agavoides ('Lipstick', 'Ebony')", "Pointed, firm leaves; 'Lipstick' has red edges, 'Ebony' dark tips."),
            Variety("E. laui", "Thick, round, chalk-white leaves; slow and loved by collectors."),
            Variety("'Lola'", "Hybrid with a pale lavender, almost translucent glow."),
            Variety("E. pulvinata (chenille plant)", "Fuzzy, velvety leaves on short stems; orange flowers."),
            Variety("E. runyonii 'Topsy Turvy'", "Leaves curl upward and back like spoons."),
        ),
        funFacts = listOf(
            "Echeveria is named after Atanasio Echeverría y Godoy, an 18th-century Mexican botanical artist.",
            "The powder on many Echeveria leaves is a natural sunscreen — touch it and it's gone for good.",
            "A single Echeveria leaf can grow into a whole new plant.",
            "Echeverias redden in strong sun and dry spells: the colour is a stress pigment, like a tan.",
            "Hummingbirds pollinate many Echeveria flowers in the wild.",
        ),
        sources = listOf("World of Succulents — Echeveria", "Mountain Crest Gardens care guides", "Wikipedia — Echeveria", "LLIFLE — Echeveria"),
        keywords = listOf("echeveria", "echevaria", "echievera", "echeviera", "echeveira", "perle von", "lola"),
    )

    val gymnoMiha = PlantInfo(
        id = "gymnocalycium-mihanovichii",
        name = "Gymnocalycium mihanovichii",
        otherNames = "Gymno miha · Moon cactus / Hibotan (緋牡丹) · chin cactus",
        origin = "Paraguay (Gran Chaco), and nearby Bolivia and Argentina",
        summary = "The small banded 'chin cactus' — and, in red and yellow, the grafted moon cactus.",
        description = "A small, flattened globe with about eight ribs, grey-green to purplish with paler bands, " +
            "and a 'chin' under each areole. The famous coloured forms — red, orange, yellow, pink — were selected " +
            "in Japan as 'Hibotan'. They have little or no chlorophyll, so they cannot feed themselves and live " +
            "grafted onto a green cactus, usually dragon fruit (Hylocereus). Partly green, variegated forms " +
            "('Hibotan nishiki') can grow on their own roots.",
        sun = "Bright light with protection from harsh midday sun — gymnos are among the cacti that like some " +
            "shade. Coloured moon cacti burn easily: bright shade or gentle morning sun only.",
        water = "Soak and let dry, roughly every 7–14 days in warm weather. A grafted moon cactus is really " +
            "watered for its rootstock: dragon fruit likes a bit more water and warmth than most cacti, but still " +
            "hates soggy soil.",
        tips = listOf(
            "Moon cacti often live only a few years because the graft or the stock gives out; regraft a healthy top onto fresh stock.",
            "Remove any shoots that grow from the green rootstock — they steal its strength.",
            "Keep above about 10 °C: the dragon-fruit stock suffers in cold.",
            "Flowers come readily in spring-like conditions: pink or greenish-white, from the top.",
        ),
        varieties = listOf(
            Variety("Hibotan (red)", "The classic moon cactus; no chlorophyll, must be grafted."),
            Variety("Yellow, orange and pink Hibotan", "Same, in other colours; pink and dark red are often colour selections."),
            Variety("Hibotan nishiki (variegated)", "Patches of green with red or yellow; can live on own roots if green enough."),
            Variety("var. friedrichii", "Darker, more purple bodies with silver-banded ribs; the base of many 'LB' plants."),
        ),
        funFacts = listOf(
            "Gymnocalycium means 'naked bud': its flower buds have no hair or spines.",
            "Red moon cacti have no chlorophyll at all — they live only because they're grafted onto a green cactus.",
            "The coloured moon cactus 'Hibotan' was bred in Japan; the name means 'scarlet peony'.",
            "The 'chins' under each areole give Gymnocalycium its nickname, the chin cactus.",
        ),
        sources = listOf("LLIFLE — Gymnocalycium mihanovichii", "Wikipedia — Gymnocalycium mihanovichii", "cactus-art.biz — Gymnocalycium"),
        keywords = listOf("mihanovichii", "gymno miha", "hibotan", "moon cactus", "kaktus bulan", "chin cactus"),
    )

    val gymnoLbVos = PlantInfo(
        id = "gymnocalycium-lb-vos",
        name = "Gymno LB and VoS",
        otherNames = "Gymnocalycium friedrichii LB 2178 · VoS field-number gymnos",
        origin = "Agua Dulce, Alto Paraguay, Paraguay (LB 2178) and other Chaco sites",
        summary = "Collector gymnos named after the field number of the wild plants they came from.",
        description = "LB and VoS are not species but field numbers: the code a collector gives to one wild " +
            "population. VoS numbers come from Volker Schädlich; LB numbers from Ludwig Bercht, a Gymnocalycium " +
            "specialist. LB 2178, from Agua Dulce in Paraguay, is a narrow-ribbed, dark purple-brown form of the " +
            "friedrichii group (listed as G. friedrichii, G. mihanovichii or G. stenopleurum depending on the " +
            "source). Japanese and Thai growers selected seedlings of it for bold silver chevrons and stripes, and " +
            "crossed it with variegated Hibotan — those patterned hybrids are what most shops sell as 'Gymno LB'.",
        sun = "Bright light with shade from harsh midday sun. More sun deepens the purple and the contrast of " +
            "the pattern, but scorches quickly; morning sun is ideal.",
        water = "Soak and let dry, about every 7–14 days in warm weather; less when cool or dull. Grafted " +
            "plants follow their rootstock's needs.",
        tips = listOf(
            "Ask for the field number on the label: 'LB 2178' or 'VoS 01-014' tells you where the line came from.",
            "Seedlings vary a lot — buy the plant you see, not the name.",
            "Many are grafted while young for speed, then cut and rooted later; own-root plants are slower but longer-lived.",
            "A gritty mix with some organic matter; gymnos like a little more humus than desert cacti.",
        ),
        varieties = listOf(
            Variety("LB 2178", "Dark, narrow ribs with pale bands; the parent of most patterned 'LB' hybrids."),
            Variety("LB 2178 × Hibotan variegata", "Variegated hybrids in pink, yellow and dark patches; slightly wider ribs."),
            Variety("VoS 01-014a (friedrichii var. moserianum)", "A Schädlich field number from east of Agua Dulce."),
        ),
        funFacts = listOf(
            "'LB 2178' is a field number, not a name: it marks one wild population in Agua Dulce, Paraguay.",
            "VoS field numbers come from Volker Schädlich, who collected Gymnocalycium in Paraguay.",
            "Botanists still argue whether LB 2178 belongs to G. friedrichii, G. mihanovichii or G. stenopleurum.",
            "The bold chevron patterns on 'Gymno LB' come from years of seedling selection by Japanese and Thai growers.",
        ),
        sources = listOf(
            "pukubook.jp — Gymnocalycium mihanovichii LB2178",
            "cactus-art.biz — G. friedrichii v. angustostriatum LB2178; VoS-014A",
            "Chiba University plant database — G. stenopleurum",
        ),
        keywords = listOf("lb2178", "lb 2178", "gymno lb", "lb-2178", "vos ", "vos0", "vos-", "gymno vos"),
    )

    val coryphantha = PlantInfo(
        id = "coryphantha",
        name = "Coryphantha",
        otherNames = "Beehive cactus · relatives: Mammillaria, Escobaria, Neolloydia (tribe Cacteae)",
        origin = "Mexico, and the south-western United States",
        summary = "Big-nippled globes with a groove on top of each tubercle, and large flowers on the crown.",
        description = "Coryphantha are globes or short columns covered in large tubercles. The tell-tale sign is " +
            "a groove running along the top of each mature tubercle — that's where the flowers come from, right " +
            "at the crown, which gives the name: 'flowering at the summit'. Their close family is the tribe " +
            "Cacteae: Mammillaria (flowers come from between the tubercles instead), Escobaria (once lumped in " +
            "with Coryphantha), Neolloydia, and more distantly Ferocactus and Echinocactus. Many species have small " +
            "red or yellow glands that ooze sweet nectar to recruit ants as bodyguards.",
        sun = "Full sun to very bright light; at least 4–6 hours direct. They take more sun than most small cacti " +
            "once used to it.",
        water = "Soak and let dry completely, about every 10–14 days in warm weather. Many have thick, carrot-like " +
            "roots that store water and rot in wet soil — keep them on the dry side.",
        tips = listOf(
            "Deep pots for the species with taproots; very gritty mix.",
            "The ant-attracting nectar glands are normal, not a pest — but they can draw sooty mould if the plant is wet.",
            "Big yellow or pink flowers come in summer on mature plants.",
        ),
        varieties = listOf(
            Variety("C. elephantidens", "Huge, rounded 'elephant tooth' tubercles and big pink flowers."),
            Variety("C. erecta", "Clumping short columns with golden spines and yellow flowers."),
            Variety("C. cornifera / C. radians", "Globes with comb-like spines and yellow flowers."),
            Variety("C. werdermannii", "Dense, white, interlacing spines; slow."),
            Variety("Escobaria (relative)", "Smaller, often clustering cousins, sometimes still sold as Coryphantha."),
        ),
        funFacts = listOf(
            "Coryphantha means 'flowering at the summit' — its flowers come from the very top of the plant.",
            "Some Coryphantha ooze nectar from little glands to hire ants as bodyguards.",
            "A groove on top of each tubercle tells Coryphantha apart from its cousin Mammillaria.",
            "Coryphantha and Mammillaria belong to the same cactus tribe, Cacteae, with barrel cacti like Ferocactus.",
        ),
        sources = listOf("LLIFLE — Coryphantha", "Wikipedia — Coryphantha", "cactus-art.biz — genus Coryphantha"),
        keywords = listOf("coryphantha", "coryphanta", "koryphanta", "elephantidens", "escobaria"),
    )

    val kikko = PlantInfo(
        id = "astrophytum-kikko",
        name = "Astrophytum asterias 'Kikko'",
        otherNames = "Astro kikko · 亀甲 'tortoise shell' Kabuto",
        origin = "Japanese cultivar of the Texas–Mexico sea urchin cactus",
        summary = "A sea urchin cactus whose ribs break up into a tortoise-shell pattern.",
        description = "'Kikko' (亀甲) means tortoise shell. In these plants the eight smooth ribs of a normal " +
            "asterias are broken across into separate, often hexagonal tubercles, so the dome looks like a turtle's " +
            "shell. It is a Japanese selection and is combined with other traits — 'Super Kabuto' flecking, " +
            "variegation, V-shaped markings — in endless named crosses. The trait is only partly inherited: " +
            "seedlings of kikko parents vary, and the best are picked out.",
        sun = "Like other asterias: strong light with shade from harsh midday and afternoon sun. Variegated kikko " +
            "need bright shade and gentle morning sun only.",
        water = "Very carefully: only when the soil is completely dry, roughly every 2–3 weeks in warm weather, " +
            "and little in cool or dull spells. It rots as easily as any asterias.",
        tips = listOf(
            "Mineral, fast-draining mix and a small pot; never water onto the body.",
            "Often grafted while young for speed — check whether you're buying a graft or own-root plant.",
            "Astrophytum needs pollen from a different plant to set seed; two kikko plants can give kikko seedlings.",
        ),
        varieties = listOf(
            Variety("Kikko", "Tubercles in a tortoise-shell pattern, dark skin with few flecks."),
            Variety("Super Kabuto Kikko", "The same pattern, densely covered in white flecks."),
            Variety("Kikko variegata", "With yellow or cream variegation; slower and burns easily."),
            Variety("Myriostigma kikko", "The trait also appears in bishop's cap (A. myriostigma)."),
        ),
        funFacts = listOf(
            "'Kikko' is Japanese for tortoise shell — the ribs break into a pattern like a turtle's back.",
            "Only some seedlings of kikko parents come out kikko; growers pick the best from big sowings.",
            "Astrophytum can't pollinate itself: it needs pollen from a second plant to make seed.",
            "Japanese growers have bred Astrophytum asterias for generations into hundreds of named forms.",
        ),
        sources = listOf("LLIFLE — Astrophytum asterias cultivars", "cactus-art.biz — Astrophytum asterias 'Kikko'", "World of Succulents"),
        keywords = listOf("kikko", "kiko", "kikkou", "tortoise"),
    )

    val mammillaria = PlantInfo(
        id = "mammillaria",
        name = "Mammillaria",
        otherNames = "Pincushion cactus · nipple cactus · about 200 species",
        origin = "Mostly Mexico; also the south-western US, Caribbean, Colombia and Venezuela",
        summary = "Small pincushions that flower in a ring around the top.",
        description = "One of the biggest cactus genera. Instead of ribs, the body is covered in nipple-like " +
            "tubercles (the name comes from Latin 'mamilla', nipple), with spines on their tips. Flowers come " +
            "from the gaps between tubercles and often form a crown or ring around the top, followed by red, " +
            "club-shaped fruits. Many species cluster into mounds. Most are easy and flower young.",
        sun = "Bright light with some direct sun — about 4–6 hours, shaded from the harshest midday sun. Dense " +
            "white-spined species take more sun; soft green ones less.",
        water = "Soak and let dry, about every 7–14 days in warm weather. Less in cool or cloudy spells. Hairy " +
            "and woolly species should be watered at the soil, not overhead.",
        tips = listOf(
            "Gritty mix and a wide, shallow pot for clustering species.",
            "Several species (M. bocasana, M. elongata relatives) have hooked spines that catch skin — handle with a folded paper.",
            "Look between the tubercles for white fluff that isn't the plant's own wool: that's mealybug.",
            "A drier, slightly cooler rest helps flowering, but many flower in the tropics anyway.",
        ),
        varieties = listOf(
            Variety("M. elongata (ladyfinger)", "Finger-like stems in clumps, golden spines."),
            Variety("M. plumosa (feather cactus)", "Completely hidden under soft, white, feathery spines."),
            Variety("M. bocasana (powder puff)", "Silky white hairs with hooked central spines; cream flowers."),
            Variety("M. hahniana (old lady cactus)", "White hair-like bristles and a ring of pink flowers."),
            Variety("M. spinosissima 'Un Pico'", "One long spine per tubercle; bright magenta flowers."),
            Variety("M. gracilis 'Fragilis' (thimble cactus)", "Tiny stems that break off and root easily."),
            Variety("M. carmenae", "Soft, star-shaped cream to yellow spines; white or pink flowers."),
        ),
        funFacts = listOf(
            "Mammillaria comes from the Latin 'mamilla', nipple, after its nipple-shaped tubercles.",
            "Mammillaria flowers often open in a ring, like a crown around the top of the plant.",
            "With about 200 species, Mammillaria is one of the largest cactus genera.",
            "Mammillaria fruits are little red clubs that are edible, though tiny.",
        ),
        sources = listOf("LLIFLE — Mammillaria", "Wikipedia — Mammillaria", "World of Succulents — Mammillaria"),
        keywords = listOf("mammillaria", "mamillaria", "mamilaria", "mammilaria", "pincushion", "plumosa", "elongata", "bocasana"),
    )

    val koboi = PlantInfo(
        id = "kaktus-koboi",
        name = "Kaktus koboi",
        otherNames = "Cowboy cactus · a shop name for tall, branching columns",
        origin = "Depends on the plant: the Americas (true cacti) or Africa (euphorbias)",
        summary = "A trade name, not a species — the 'wild west' column with upturned arms.",
        description = "'Kaktus koboi' is what Indonesian shops call tall columnar plants with arms that curve " +
            "upward, like the saguaro in cowboy films. It is not one species. Some are true cacti — Cereus, " +
            "Stenocereus, Pachycereus, from Mexico and the Americas. Many others are euphorbias from Africa " +
            "(Euphorbia ingens, E. trigona, E. lactea) that only look like cacti. Check before you buy: euphorbias " +
            "bleed white, milky sap when cut or scratched, and that sap irritates skin and eyes; true cacti have " +
            "clear sap and spines growing from fuzzy areoles.",
        sun = "Full sun to very bright light for both kinds; indoors, the brightest window. Move into hard sun " +
            "gradually.",
        water = "Soak and let dry, about every 10–14 days in hot weather; much less when cool. Tall plants in small " +
            "pots dry fast but rot at the base if left wet.",
        tips = listOf(
            "Use a heavy terracotta or concrete pot so a tall plant doesn't topple.",
            "With euphorbias, wear gloves and keep the milky sap away from eyes; rinse with plenty of water if it splashes.",
            "Cut tops of both kinds can be dried for a week and rooted to make a new plant.",
            "Brown, corky skin at the base of old columns is normal ageing, not rot, as long as it is hard.",
        ),
        varieties = listOf(
            Variety("Cereus (e.g. C. hildmannianus)", "True cactus: blue-green ribbed columns, big white night flowers."),
            Variety("Stenocereus / Pachycereus", "True cacti: thick, tree-like columns from Mexico."),
            Variety("Euphorbia ingens / E. trigona", "Euphorbias: candelabra shape, small paired spines, milky sap."),
            Variety("Euphorbia lactea", "Euphorbia: triangular stems with a pale marbled stripe; crested forms are common."),
        ),
        funFacts = listOf(
            "Many 'cowboy cacti' sold in shops aren't cacti at all, but African euphorbias that evolved the same shape.",
            "Cut a euphorbia and it bleeds milky sap; cut a true cactus and the sap is clear.",
            "The saguaro in cowboy films can take 50 years or more to grow its first arm.",
            "Euphorbias and cacti looking alike is a classic case of convergent evolution: same desert, same solution.",
        ),
        sources = listOf("Orami — Kaktus koboi", "Wikipedia — Ceroid cactus; Euphorbia ingens", "US National Park Service — Saguaro"),
        keywords = listOf("koboi", "cowboy", "kaktus koboi"),
    )
}
