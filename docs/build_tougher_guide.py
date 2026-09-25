"""Concise player guide for the Tougher mod."""

from reportlab.lib import colors
from reportlab.lib.enums import TA_JUSTIFY
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import (
    KeepTogether,
    ListFlowable,
    ListItem,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
)

OUT = r"C:\client\grok\minecraft\docs\Tougher.pdf"
INK = colors.HexColor("#1c1917")
ACCENT = colors.HexColor("#3f6212")
RULE = colors.HexColor("#a3a3a3")


def styles():
    base = getSampleStyleSheet()
    return {
        "title": ParagraphStyle(
            "Title",
            parent=base["Title"],
            fontName="Times-Bold",
            fontSize=22,
            leading=26,
            textColor=ACCENT,
            spaceAfter=2,
        ),
        "sub": ParagraphStyle(
            "Sub",
            parent=base["Normal"],
            fontName="Times-Italic",
            fontSize=11,
            leading=14,
            textColor=colors.HexColor("#44403c"),
            spaceAfter=10,
        ),
        "h": ParagraphStyle(
            "H",
            parent=base["Heading2"],
            fontName="Times-Bold",
            fontSize=13,
            leading=16,
            textColor=ACCENT,
            spaceBefore=11,
            spaceAfter=3,
        ),
        "body": ParagraphStyle(
            "Body",
            parent=base["Normal"],
            fontName="Times-Roman",
            fontSize=10,
            leading=13,
            textColor=INK,
            alignment=TA_JUSTIFY,
            spaceAfter=4,
        ),
        "bullet": ParagraphStyle(
            "Bullet",
            parent=base["Normal"],
            fontName="Times-Roman",
            fontSize=10,
            leading=13,
            textColor=INK,
            leftIndent=0,
        ),
    }


def bullets(items, body):
    return ListFlowable(
        [ListItem(Paragraph(text, body), leftIndent=12, bulletColor=ACCENT) for text in items],
        bulletType="bullet",
        start="circle",
        leftIndent=14,
        bulletFontName="Times-Roman",
        bulletFontSize=8,
        spaceBefore=1,
        spaceAfter=2,
    )


def footer(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(RULE)
    canvas.setLineWidth(0.4)
    canvas.line(0.7 * inch, 0.5 * inch, letter[0] - 0.7 * inch, 0.5 * inch)
    canvas.setFont("Times-Roman", 8)
    canvas.setFillColor(colors.HexColor("#57534e"))
    canvas.drawString(0.7 * inch, 0.34 * inch, "Tougher")
    canvas.drawRightString(letter[0] - 0.7 * inch, 0.34 * inch, str(doc.page))
    canvas.restoreState()


def build():
    s = styles()
    story = [
        Paragraph("Tougher", s["title"]),
        Paragraph("A concise guide to the survival rules.", s["sub"]),
        Paragraph(
            "Tougher makes ordinary survival stricter: tools wear out, crops fail, hunger only falls while you act, "
            "and mana abilities, achievements, and village homes sit on top of that. "
            "In game, type <font face='Courier'>/tougher help</font> for topics, "
            "<font face='Courier'>/tougher ability help</font> for abilities, "
            "and <font face='Courier'>/tougher me</font> for your own mana and skills. "
            "Hold an ability item and press ? for that ability.",
            s["body"],
        ),
        Paragraph("Mining and tools", s["h"]),
        bullets(
            [
                "Stone and deepslate need an iron pickaxe or better. Breaking nearby coal or ore can soften stone.",
                "Wood, stone, and copper tools last one-third as long as usual.",
            ],
            s["bullet"],
        ),
        Paragraph("Building", s["h"]),
        bullets(
            [
                "You cannot place a solid block while in the air, including while standing on the very edge of a block, or with no support.",
                "Master Builder suspends those two limits while it runs. Right-click stone bricks in the air to cast it. Torch rules stay.",
                "Torches, lanterns, and glowstone cannot be carried in the off hand.",
                "Bucketed water evaporates. Use ice if you need a water source.",
                "A boat that falls more than 3 blocks with you in it breaks and drops nothing.",
                "Breaking a block that is on fire sets you on fire, the same as putting the fire out by hand. Water still works.",
                "Sneak with a compass to point it at your bed or respawn anchor. With none set, it still points at world spawn.",
                "A bed placed in the Nether or the End vanishes. It does not explode. A bed already there is removed instead of exploding if you use it.",
            ],
            s["bullet"],
        ),
        Paragraph("Torches and campfires", s["h"]),
        bullets(
            [
                "Placed torches need airflow and a firm block. Campfires need airflow too.",
                "Torches burn out after 7 Minecraft days and start dimming after 2. Light is 14 minus (days burning minus 2). Copper torches last twice as long.",
                "Right-click a torch with coal or charcoal to add 40 days (80 on copper). That does not make it permanent.",
                "Look at a torch or campfire to see the time left. Redstone torches are permanent: they do not burn, dim, or show a duration.",
                "Breaking a torch with less than 3 days left destroys it. Permanent torches still drop.",
                "Campfires also last 7 days unless they pull a log from a chest within 12 blocks, which adds 7 more days.",
            ],
            s["bullet"],
        ),
        Paragraph("Hunger", s["h"]),
        Paragraph(
            "Hunger drops while you move or act, not while you stand still. "
            "A food you have not eaten in your last 7 meals gives +1 hunger, or +1 saturation if the bar is already full. "
            "Seven different foods in the last 7 meals gives another +1 hunger, +1 saturation, and 3 experience, "
            "and it can happen again on later meals that keep that streak. "
            "The same food 5 times in the last 7 meals restores less; 7 of the last 7 restores even less.",
            s["body"],
        ),
        Paragraph("Well Fed", s["h"]),
        Paragraph(
            "After those seven different meals, each longer run of different foods raises Well Fed by one. "
            "Eight different meals in a row is Well Fed 1. Twelve is Well Fed 5. It can go higher. "
            "At Well Fed 3 the game looks at your last 13 meals and allows 3 repeats before the level drops. "
            "That grace grows with every level. The same-food penalty also waits one extra repeat per level. "
            "Losing a level removes exactly that level's benefits. Health and hunger clamp down. The mana level leaves, but mana you already hold is kept. That is not an extra hit.",
            s["body"],
        ),
        Paragraph("These stop at Well Fed 5:", s["body"]),
        bullets(
            [
                "2% less saturation loss per level, up to 10%.",
                "+1 melee damage at level 2, and another at level 4.",
                "Food regen is 1 second faster at level 2, and 2 seconds faster at 4 and 5, and never faster than once every 10 seconds.",
                "Luck I from level 3 on.",
                "Poison and Hunger last 10% less per level, up to half.",
            ],
            s["bullet"],
        ),
        KeepTogether([
            Paragraph("These keep rising past 5, and leave when Well Fed drops:", s["body"]),
            bullets(
            [
                "+1 health point per level, arriving filled. Odd totals show a half heart outline.",
                "+1 hunger per level, arriving filled. You can keep eating until that higher bar is full. Eating is no longer blocked at 20.",
                "+1 mana level per level. It does not arrive filled, and eating does not restore mana.",
            ],
            s["bullet"],
        ),
        ]),
        Paragraph(
            "Creative mode and spectators do not get Well Fed.",
            s["body"],
        ),
        Paragraph("Crops, trees, and hives", s["h"]),
        bullets(
            [
                "Crops grow slowly. Sugar cane is one-tenth speed. Nether wart is one-twentieth. Trees take 10 times as long, and leaves drop half as many saplings.",
                "A one-block cane that would grow a second segment may become a weed, using the current crop-loss rate plus the soil under it.",
                "Pumpkins and melons fruit at one-tenth speed. After each fruit the vine may become a weed, starting at 0% and rising 5% per fruit. Broken vines drop no seeds.",
                "Shearing a full hive gives 1 honeycomb.",
            ],
            s["bullet"],
        ),
        Paragraph("Soil", s["h"]),
        Paragraph(
            "Unworked soil shows 0. Look at farmland, a crop, or sugar cane to see the plot's modifier. "
            "Green and positive is good. Red and negative is bad. "
            "The first till scores water sources within 2 blocks, up to 50. Past 50, each extra source subtracts 1, down to 0. "
            "A roll of 1 to 10 is then subtracted, and twice the hoe quality is added: wood or stone 0, copper 2, iron 4, diamond 6, gold 8, netherite 10. "
            "A hoe that breaks on that till still counts. "
            "Later hoeing steps the plot toward the hoe's target (wood or stone -10, copper -5, iron 0, diamond +10, gold +20, netherite +30). "
            "A worsening step is one-tenth of the gap (at least 1, at most 4). An improving step is 3. "
            "Harvesting without a hoe subtracts 5. Replanting the same crop subtracts 5. "
            "Let it grow adds 3 under each plant it grows. Bone meal adds 5.",
            s["body"],
        ),
        Paragraph("Seasons", s["h"]),
        Paragraph(
            "By default, crop loss falls 1% per day from the normal rate down to half, then rises to three times that rate, then repeats. "
            "Growth never exceeds vanilla, and ripe food crops can die at the seasonal loss plus the plot's soil modifier. "
            "Above 25% loss, cows, sheep, and pigs drop 1 less meat. "
            "Above 60%, those animals drop 2 less, chickens drop 1 less, bees stay in their hives, and the air looks blighted. "
            "Let it grow does not cause that crop death.",
            s["body"],
        ),
        Paragraph("Animals and processing", s["h"]),
        bullets(
            [
                "Animals wait 6 times as long to breed. Chickens lay eggs 6 times as slowly.",
                "Once a day, livestock claim nearby grass they can walk to within 6 blocks. A large animal claims 9 patches. A chicken or rabbit claims 4.",
                "If they cannot claim enough grass, they have a 33% chance to eat breeding food from a chest they can reach within 20 blocks. Otherwise they can starve without dropping meat.",
                "Ordinary villagers take 8 times as long to restock. That is separate from residents.",
                "Composters fill normally but finish slowly. Hopper output from furnaces and composters stores cooking experience in the destination chest at 50% extra.",
            ],
            s["bullet"],
        ),
        Paragraph("Combat", s["h"]),
        bullets(
            [
                "Mobs hit harder. Skeletons ignore arrows. Zombies vary about 20% in speed: faster ones hit softer and look smaller.",
                "Skeleton and bogged special shots are 1% per 10 blocks from world spawn, and certain at 1,000 blocks or farther.",
                "Zombies can rise again. Shields absorb the whole blocked hit and last one-third as long.",
                "Creepers may drop live TNT. TNT sets fire to 20% of the blocks it hits, rounded. Set explosions.tnt.firePercent to 0 to turn that off.",
                "Breaking a burning block sets you on fire, as above.",
            ],
            s["bullet"],
        ),
        KeepTogether([
            Paragraph("Biome bosses", s["h"]),
            Paragraph(
            "A strong boss can appear far from spawn, once per biome family. After it is defeated it does not return in that game. "
            "Bosses do not spawn within 80 blocks of a player or a chest. "
            "A brood mother keeps chasing when hit, and every few seconds spits for half her bite damage and a brief blindness. "
            "If a boss cannot walk to you, it breaks blocks in the way at iron-pickaxe speed. "
            "Each defeat makes the next boss 30% harder and richer. The third is a world event. "
            "The seventh rolls the credits, and you can keep playing.",
            s["body"],
            ),
        ]),
        Paragraph("Armor", s["h"]),
        Paragraph(
            "A full set of heavy copper, iron, diamond, or netherite armor adds 5, 10, 20, or 30 hearts. "
            "Those pieces last twice as long as the matching vanilla pieces. "
            "Healing and food regen fill every heart, not only the first ten.",
            s["body"],
        ),
        Paragraph("Mana", s["h"]),
        Paragraph(
            "Mana is shown as cyan crystals. Two mana points fill one crystal. "
            "You regenerate (mana level + current mana) / 200 per minute. "
            "Nether quartz in your inventory makes that 3 times faster until you reach your mana level, using 1 quartz per 2 mana. "
            "A quartz block in your main inventory makes it 7 times the base rate until you reach your mana level, using 1 block per 2 mana. "
            "Both together are 10 times, and each is consumed only for its own share. "
            "If your mana is below your mana level and below saturation minus 17, you also restore 1 mana per minute for 1 saturation. That does not spend quartz. "
            "Past your level, regeneration is one-quarter speed. The old ceiling of 20 mana rises when Well Fed pushes your level past 20. "
            "Achievements and wise teachers can raise the permanent mana level. Well Fed adds levels on top, and those extra levels leave when Well Fed drops.",
            s["body"],
        ),
        Paragraph("Abilities", s["h"]),
        Paragraph(
            "Hold the catalyst and right-click. Power starts as the square root of how many times you have used that ability. "
            "Extra catalyst and a piece of redstone dust each add 2, and the redstone is consumed. "
            "Fire bolt and Magic arrow also add your mana level. "
            "Each gold armor piece worn for 30 seconds adds 1. A golden item in either hand, such as a golden hoe, adds 1 more after 30 seconds. "
            "Gold ingots, nuggets, blocks, and ore do not. The bonus ends the moment that item comes off, and a half-second golden shimmer plus "
            "\"Ability level +1 (+total)\" plays when a piece starts counting. "
            "A golden hoe still keeps its usual Let it grow material bonus on top of that.",
            s["body"],
        ),
        bullets(
            [
                "Paper: Healing. Iron ingot: Iron Heart. Feather: Flight. Charcoal: Fire bolt. Arrow: Magic arrow.",
                "Any hoe: Let it grow, on a plant. Coal: Let there be light. Any pickaxe: Power mining. Compass: Detect ore.",
                "String: Slow. Spider eye: Sense Evil. Golden sword: Smite Evil. Stone bricks in the air: Master Builder. Diamond: Diamond Skin.",
                "Master Builder lasts 30 seconds times ability level, renews while you have mana, and cancels on another air click. An extra brick in hand is consumed for +2.",
                "Diamond Skin costs 2 mana and lasts 30 seconds times ability level. Its effective level is at least 2, so a bare cast still halves health damage. It renews while you have 2 mana, and cancels on another right-click. An extra diamond in hand is consumed for +2.",
                "While Diamond Skin lasts, damage that would reach your health is divided by the ability level. Armor still applies first. Each point prevented shortens the effect by 3 seconds. Poison, drowning, and suffocation are not reduced. A quarter-second cyan sparkle plays when a hit is reduced.",
                "You can learn up to half your mana level in abilities, rounded up, by using them. Unlearned use still gains skill, at -3 power, floored at 1, until a slot is free.",
                "A wise teacher in a 24-point home teaches one ability for 30 emeralds. Each house point above 24 takes 2 emeralds off, down to free. The lesson ignores the learning cap and does not use a self-learned slot. Paying for an ability you already know frees that slot.",
                "Sneak with a diamond block and a lapis lazuli block and that teacher raises your permanent mana by 1, once per teacher.",
            ],
            s["bullet"],
        ),
        Paragraph("Achievements", s["h"]),
        bullets(
            [
                "Place many of one block, or defeat many of one monster that drops loot. Each claim gives 50 experience and a diamond. Everyone can earn every achievement.",
                "The first player on the server to claim one gets 25 extra experience by default.",
                "You have your experience level as a percent chance to gain a mana level.",
                "A diamond block plus lapis lazuli blocks buys an extra mana level and a 6-second blue shimmer. The lapis cost starts at 1 block and rises by 1 each time.",
                "The first visit to a biome is 50 experience, and 100 more if you are first on the server. The first habitable home in a biome is house points times 3. The first trip 300 blocks from spawn is 100 experience.",
            ],
            s["bullet"],
        ),
        Paragraph("Homes and residents", s["h"]),
        Paragraph(
            "Right-click a bed with a clock to score a room. An enclosed, lit room of 24 to 300 air blocks, with a bed and a door or fence gate facing outside, "
            "can attract one resident if it has at least 12 furnishing points and stays 48 blocks from another occupied home. "
            "Furnishings come from size, extra rooms, windows, art, rugs, seating, storage, workstations, lights, plants, books, a second bed, and a kitchen. "
            "Empty eligible homes are checked at dawn. Chance starts at 8% at 12 points and rises with the score. It is halved in blight. "
            "Chests bias a hauler, a kitchen biases a cook, and plants or workstations bias a farmer. "
            "18 points can bring a bounty board or a wealthy trader. 24 can bring an armorsmith or a wise teacher. 30 can bring a master armorsmith. "
            "A council member can appear in any eligible home and is especially likely. New residents prefer a specialty that has not appeared yet.",
            s["body"],
        ),
        bullets(
            [
                "Most residents restock every 14 Minecraft days. Armorsmiths restock every 30. Look at one who is waiting to see the time left, in days and hours.",
                "Shop size follows house points. The top of the roll is 50% at 14 points and 175% at 38. The bottom is half of that. Prices do not inflate.",
                "Residents have 40 health plus their house points, and regain 1 health a minute. Looking at one shows hearts, one per 10 health, beside the restock time.",
                "They shoot mobs that come within 12 blocks and are after them. The bow hits 3 harder than a normal arrow. A close attack makes them switch to an iron sword.",
                "A zombie cannot turn a resident unless the hit that kills them starts while they are already below 5 health.",
                "Council members give each player a personal kill bounty: 6 to 18 common mobs at first, 30% larger and rarer after each success, with 7 Minecraft days to finish. The reward is experience from the slain health, plus emeralds when you return.",
            ],
            s["bullet"],
        ),
    ]
    doc = SimpleDocTemplate(
        OUT,
        pagesize=letter,
        leftMargin=0.7 * inch,
        rightMargin=0.7 * inch,
        topMargin=0.65 * inch,
        bottomMargin=0.65 * inch,
        title="Tougher",
        author="Tougher",
    )
    doc.build(story, onFirstPage=footer, onLaterPages=footer)


if __name__ == "__main__":
    build()
