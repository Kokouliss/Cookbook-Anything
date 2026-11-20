package com.cookbookanything.core.domain.shopping

import com.cookbookanything.core.domain.search.searchKey

/** The part of the shop an ingredient is found in; the shopping list is grouped by it. */
enum class Aisle {
    PRODUCE, DAIRY_EGGS, MEAT_FISH, BAKERY, PASTA_GRAINS, FLOUR_SUGAR, OILS_VINEGARS, SAUCES, HERBS_SPICES, PANTRY, FREEZER, OTHER
}

private val whitespace = Regex("""\s+""")

// Word starts, without accents: "tomato" matches "tomatoes", "ντοματ" matches "ντομάτες". A phrase
// ("olive oil") must appear whole. The longest match wins, so "olive oil" beats "olive".
private val stems: List<Pair<String, Aisle>> = listOf(
    Aisle.PRODUCE to """tomato onion garlic potato carrot lemon lime orange apple banana cucumber zucchini courgette aubergine
        eggplant peppers$ bell_pepper red_pepper green_pepper chili lettuce spinach kale cabbage broccoli cauliflower celery leek
        mushroom avocado ginger scallion spring_onion shallot
        pea green_bean corn sweet_potato pumpkin squash beet radish fennel artichoke asparagus okra olive strawberr berr
        grape peach pear plum cherr fig$ melon watermelon pomegranate mango pineapple kiwi apricot date$ fruit vegetable salad
        ντοματ κρεμμυδ σκορδ πατατ καροτ λεμον λαιμ πορτοκαλ μηλ μπαναν αγγουρ κολοκυθ μελιτζαν πιπερια πιπεριες τσιλι μαρουλ
        σπανακ λαχαν μπροκολ κουνουπιδ σελερ σελιν πρασ μανιταρ αβοκαντ τζιντζερ φρεσκο_κρεμμυδ αρακ φασολακ καλαμποκ γλυκοπατατ παντζαρ ραπαν μαραθ αγκιναρ σπαραγγ μπαμι ελιες ελια φραουλ
        σταφυλ ροδακιν αχλαδ δαμασκην κερασ συκ πεπον καρπουζ ροδ μανγκ ανανα ακτινιδ βερικοκ φρουτ λαχανικ σαλατ ροκα""",
    Aisle.DAIRY_EGGS to """egg milk$ butter$ cream$ yogurt yoghurt cheese feta mozzarella parmesan halloumi ricotta mascarpone gruyere cheddar
        kefalotyri graviera anthotyro manouri cream_cheese sour_cream double_cream heavy_cream whipping_cream creme_fraiche
        αυγ γαλα βουτυρ κρεμα_γαλακτος γιαουρτ τυρ φετα μοτσαρελ παρμεζαν χαλουμ ρικοτ μασκαρπον γραβιερ κεφαλοτυρ ανθοτυρ
        μανουρ κασερ μυζηθρ τυρι_κρεμα ξινοκρεμ κρεμα_γαλακτ""",
    Aisle.MEAT_FISH to """chicken beef pork lamb veal turkey sausage bacon ham mince minced_meat ground_beef ground_pork steak fillet
        fish salmon tuna cod shrimp prawn squid octopus mussel anchov sardine seafood meat
        κοτοπουλ μοσχαρ χοιριν αρνι κατσικ γαλοπουλ λουκανικ μπεικον ζαμπον κιμα μπριζολ φιλετ ψαρι σολομ τονο μπακαλιαρ
        γαριδ καλαμαρ χταποδ μυδι αντζουγ σαρδελ θαλασσιν κρεας παιδακ σνιτσελ""",
    Aisle.BAKERY to """bread baguette pita tortilla wrap bun roll$ brioche croissant breadcrumb rusk phyllo filo puff_pastry pastry
        ψωμι μπαγκετ πιτα τορτιγ αραβικ ψωμακ τσουρεκ κρουασαν φρυγανι φρυγανια φυλλο_κρουστ φυλλο_σφολιατ σφολιατ παξιμαδ""",
    Aisle.PASTA_GRAINS to """pasta spaghetti penne rigatoni tagliatelle linguine fusilli macaroni lasagne lasagna noodle orzo rice$ risotto
        quinoa bulgur couscous oat barley lentil chickpea bean$ ζυμαρικ μακαρον σπαγγετ σπαγκετ πεννες κριθαρακ χυλοπιτ
        λαζανι νουντλ ρυζι κινοα πλιγουρ κουσκους βρωμ κριθαρ φακες φακη ρεβυθ φασολ γιγαντ""",
    Aisle.FLOUR_SUGAR to """flour sugar icing_sugar brown_sugar caster_sugar yeast baking_powder baking_soda bicarbonate cocoa chocolate
        cornstarch cornflour semolina vanilla honey syrup maple_syrup molasses αλευρ ζαχαρ ζαχαρη_αχνη μαγια μπεικιν σοδα
        κακαο σοκολατ κορν_φλαουρ σιμιγδαλ βανιλ μελι σιροπ πετιμεζ νισταφ""",
    Aisle.OILS_VINEGARS to """oil olive_oil vegetable_oil sunflower_oil sesame_oil vinegar balsamic λαδ ελαιολαδ ηλιελαι σησαμελαι ξιδ βαλσαμικ""",
    Aisle.SAUCES to """sauce ketchup mayonnaise mustard soy_sauce tomato_sauce tomato_paste passata tahini pesto hummus salsa
        σαλτσα κετσαπ μαγιονεζ μουσταρδ σογια_σως σως πελτε ντοματοπελτ ταχιν πεστο χουμουσ""",
    Aisle.HERBS_SPICES to """salt pepper black_pepper paprika cumin cinnamon oregano thyme rosemary bay_leaf nutmeg clove turmeric
        curry chili_flake cayenne saffron cardamom allspice coriander_seed sesame spice seasoning herb parsley dill mint basil
        coriander cilantro chive αλατ πιπερι παπρικ κυμιν κανελ ριγαν θυμαρ δεντρολιβαν δαφν μοσχοκαρυδ γαρυφαλ κουρκουμ καρυ
        μπουκοβ καγιεν σαφραν κροκος καρδαμ μπαχαρ σουσαμ μπαχαρικ μαιντανο ανηθ δυοσμ βασιλικ κολιανδρ""",
    Aisle.PANTRY to """stock broth bouillon can tin canned nut almond walnut hazelnut pistachio peanut peanut_butter raisin
        dried coconut_milk coconut jam tomato_tin chickpea_tin water$ wine beer juice coffee tea
        ζωμ κυβ κονσερβ ξηροι_καρποι αμυγδαλ καρυδ φουντουκ φιστικ σταφιδ αποξηραμεν γαλα_καρυδας καρυδα μαρμελαδ νερο κρασι
        μπυρ χυμ καφε τσαι""",
    Aisle.FREEZER to """frozen ice_cream ice κατεψυγμεν παγωτ παγακ""",
).flatMap { (aisle, words) ->
    words.split(whitespace).filter { it.isNotEmpty() }.map { it.replace('_', ' ') to aisle }
}

/**
 * Where [product] is found in the shop. A stem ending in "$" must match a whole word ("rice", not
 * "ricotta"); other stems match the start of a word; phrases must appear whole. Unknown products go to [Aisle.OTHER].
 */
fun aisleFor(product: String): Aisle {
    val key = " " + product.searchKey().replace(Regex("""[^\p{L}]+"""), " ").trim() + " "
    var best: Pair<String, Aisle>? = null
    for (entry in stems) {
        val stem = entry.first
        val matches = if (stem.endsWith("$")) key.contains(" " + stem.dropLast(1) + " ") else key.contains(" $stem")
        if (matches && stem.length > (best?.first?.length ?: 0)) best = entry
    }
    return best?.second ?: Aisle.OTHER
}
