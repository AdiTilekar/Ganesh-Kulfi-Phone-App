package com.ganeshkulfi.app.data.model

data class Flavor(
    val id: String = "",
    val key: String = "",
    val nameEn: String = "",
    val nameHi: String = "",
    val nameMr: String = "",
    val descriptionEn: String = "",
    val descriptionHi: String = "",
    val descriptionMr: String = "",
    val image: String = "",
    val tags: List<String> = emptyList(),
    val price: Int = 0,
    val stock: Int = 0,
    val isAvailable: Boolean = true
) {
    companion object {
        // Default flavors matching the web app
        fun getDefaultFlavors(): List<Flavor> = listOf(
            Flavor(
                key = "mango",
                nameEn = "Mango Kulfi",
                nameHi = "आम कुल्फी",
                nameMr = "आंबा कुल्फी",
                descriptionEn = "A seasonal delight capturing the sweet, luscious taste of ripe mangoes.",
                descriptionHi = "पके आम के मीठे और रसीले स्वाद को पकड़ने वाली मौसमी खुशी।",
                descriptionMr = "पिकलेल्या आंब्याचा गोड आणि रसाळ चव असलेला हंगामी आनंद.",
                image = "mango_kulfi.png",
                tags = listOf("Fruity", "Seasonal"),
                price = 20,
                stock = 100
            ),
            Flavor(
                key = "rabdi",
                nameEn = "Rabadi Kulfi",
                nameHi = "रबड़ी कुल्फी",
                nameMr = "रबडी कुल्फी",
                descriptionEn = "Rich, thickened milk kulfi with layers of creamy rabadi.",
                descriptionHi = "मलाईदार रबड़ी की परतों के साथ गाढ़ा दूध कुल्फी।",
                descriptionMr = "मलईदार रबडीच्या थरांसह गाढ दुधाची कुल्फी.",
                image = "rabdi_kulfi.png",
                tags = listOf("Rich", "Traditional"),
                price = 20,
                stock = 80
            ),
            Flavor(
                key = "strawberry",
                nameEn = "Strawberry Kulfi",
                nameHi = "स्ट्रॉबेरी कुल्फी",
                nameMr = "स्ट्रॉबेरी कुल्फी",
                descriptionEn = "A sweet and tangy kulfi made with fresh strawberries.",
                descriptionHi = "ताज़ी स्ट्रॉबेरी से बनी मीठी और तीखी कुल्फी।",
                descriptionMr = "ताज्या स्ट्रॉबेरीपासून बनवलेली गोड आणि चवदार कुल्फी.",
                image = "strawberry_kulfi.png",
                tags = listOf("Fruity", "Modern"),
                price = 25,
                stock = 90
            ),
            Flavor(
                key = "chocolate",
                nameEn = "Chocolate Kulfi",
                nameHi = "चॉकलेट कुल्फी",
                nameMr = "चॉकलेट कुल्फी",
                descriptionEn = "A modern twist with rich, decadent chocolate for all ages.",
                descriptionHi = "सभी उम्र के लिए समृद्ध, स्वादिष्ट चॉकलेट के साथ एक आधुनिक मोड़।",
                descriptionMr = "सर्व वयोगटांसाठी समृद्ध, स्वादिष्ट चॉकलेटसह आधुनिक वळण.",
                image = "chocolate_kulfi.png",
                tags = listOf("Modern", "Rich"),
                price = 35,
                stock = 120
            ),
            Flavor(
                key = "paan",
                nameEn = "Paan Kulfi",
                nameHi = "पान कुल्फी",
                nameMr = "पान कुल्फी",
                descriptionEn = "Traditional paan flavor with a perfect blend of betel leaf essence.",
                descriptionHi = "पान के पत्ते के सार के साथ पारंपरिक पान स्वाद।",
                descriptionMr = "पानाच्या पानाच्या सारासह पारंपरिक पान चव.",
                image = "paan_kulfi.png",
                tags = listOf("Traditional", "Unique"),
                price = 25,
                stock = 70
            ),
            Flavor(
                key = "gulkand",
                nameEn = "Gulkand Kulfi",
                nameHi = "गुलकंद कुल्फी",
                nameMr = "गुलकंद कुल्फी",
                descriptionEn = "Aromatic rose petal preserve kulfi with a royal touch.",
                descriptionHi = "शाही स्पर्श के साथ सुगंधित गुलाब की पंखुड़ियों की कुल्फी।",
                descriptionMr = "राजेशाही स्पर्शासह सुगंधी गुलाबाच्या पाकळ्यांची कुल्फी.",
                image = "gulkand_kulfi.png",
                tags = listOf("Aromatic", "Premium"),
                price = 30,
                stock = 60
            ),
            Flavor(
                key = "dry_fruit",
                nameEn = "Dry Fruit Kulfi",
                nameHi = "ड्राई फ्रूट कुल्फी",
                nameMr = "ड्राय फ्रूट कुल्फी",
                descriptionEn = "Loaded with premium cashews, almonds, and pistachios.",
                descriptionHi = "काजू, बादाम और पिस्ता से भरपूर।",
                descriptionMr = "काजू, बदाम आणि पिस्त्यांनी भरलेली.",
                image = "dry_fruit_kulfi.png",
                tags = listOf("Premium", "Nuts"),
                price = 40,
                stock = 85
            ),
            Flavor(
                key = "pineapple",
                nameEn = "Pineapple Kulfi",
                nameHi = "अनानास कुल्फी",
                nameMr = "अननस कुल्फी",
                descriptionEn = "Tropical pineapple flavor with a refreshing tangy twist.",
                descriptionHi = "ताज़गी देने वाले तीखे स्वाद के साथ उष्णकटिबंधीय अनानास।",
                descriptionMr = "ताजेपणाचा चवदार स्वादासह उष्णकटिबंधीय अननस.",
                image = "pineapple_kulfi.png",
                tags = listOf("Fruity", "Tropical"),
                price = 25,
                stock = 75
            ),
            Flavor(
                key = "chikoo",
                nameEn = "Chikoo Kulfi",
                nameHi = "चीकू कुल्फी",
                nameMr = "चिकू कुल्फी",
                descriptionEn = "Sweet and smooth sapota kulfi with natural sweetness.",
                descriptionHi = "प्राकृतिक मिठास के साथ मीठी और चिकनी चीकू कुल्फी।",
                descriptionMr = "नैसर्गिक गोडीसह गोड आणि गुळगुळीत चिकू कुल्फी.",
                image = "chikoo_kulfi.png",
                tags = listOf("Fruity", "Smooth"),
                price = 22,
                stock = 65
            ),
            Flavor(
                key = "guava",
                nameEn = "Guava Kulfi",
                nameHi = "अमरूद कुल्फी",
                nameMr = "पेरू कुल्फी",
                descriptionEn = "Fresh guava kulfi with authentic desi fruit flavor.",
                descriptionHi = "प्रामाणिक देसी फल स्वाद के साथ ताज़ा अमरूद कुल्फी।",
                descriptionMr = "प्रामाणिक देशी फळांच्या चवीसह ताजी पेरू कुल्फी.",
                image = "guava_kulfi.png",
                tags = listOf("Fruity", "Desi"),
                price = 22,
                stock = 70
            ),
            Flavor(
                key = "jamun",
                nameEn = "Jamun Kulfi",
                nameHi = "जामुन कुल्फी",
                nameMr = "जांभळ कुल्फी",
                descriptionEn = "Rich purple jamun kulfi with tangy berry notes.",
                descriptionHi = "तीखे बेरी नोट्स के साथ समृद्ध बैंगनी जामुन कुल्फी।",
                descriptionMr = "चवदार बेरी नोट्ससह समृद्ध जांभळी जांभळ कुल्फी.",
                image = "jamun_kulfi.png",
                tags = listOf("Fruity", "Unique"),
                price = 28,
                stock = 55
            ),
            Flavor(
                key = "sitafal",
                nameEn = "Sitafal Kulfi",
                nameHi = "सीताफल कुल्फी",
                nameMr = "सिताफळ कुल्फी",
                descriptionEn = "Creamy custard apple kulfi with natural fruit chunks.",
                descriptionHi = "प्राकृतिक फल के टुकड़ों के साथ मलाईदार सीताफल कुल्फी।",
                descriptionMr = "नैसर्गिक फळांच्या तुकड्यांसह मलईदार सिताफळ कुल्फी.",
                image = "sitafal_kulfi.png",
                tags = listOf("Fruity", "Creamy"),
                price = 32,
                stock = 50
            ),
            Flavor(
                key = "fig",
                nameEn = "Fig Kulfi",
                nameHi = "अंजीर कुल्फी",
                nameMr = "अंजीर कुल्फी",
                descriptionEn = "Premium fig kulfi with rich, honey-like sweetness.",
                descriptionHi = "शहद जैसी मिठास के साथ प्रीमियम अंजीर कुल्फी।",
                descriptionMr = "मधासारख्या गोडीसह प्रीमियम अंजीर कुल्फी.",
                image = "fig_kulfi.png",
                tags = listOf("Premium", "Exotic"),
                price = 38,
                stock = 45
            )
        )
    }
}
