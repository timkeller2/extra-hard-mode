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
                "You cannot place a solid block while in the air, including while standing on the very edge of a block. On the ground, a bridge over a gap is allowed.",
                "Master Builder suspends that limit while it runs and adds its level to your block reach. Right-click a clay brick to cast it. Torch rules stay.",
                "Torches, lanterns, and glowstone cannot be carried in the off hand.",
                "Bucketed water and lava are not sources, except in a sealed 1x1x1 hole or receptacle that does not touch a source.",
                "A boat that falls more than 3 blocks with you in it breaks and drops nothing.",
                "Breaking a block that is on fire sets you on fire, the same as putting the fire out by hand. Water still works.",
                "Sneak with a compass to point it at your bed or respawn anchor. With none set, it still points at world spawn.",
                "A bed placed in the Nether or the End vanishes. It does not explode. A bed already there is removed instead of exploding if you use it.",
                "Breaking a bed destroys it and drops nothing: no bed, wool, or planks.",
                "A lodestone is crafted with an iron block in the middle, surrounded by chiseled stone bricks.",
            ],
            s["bullet"],
        ),
        Paragraph("Torches and campfires", s["h"]),
        bullets(
            [
                "Placed torches need airflow and a firm block. Campfires need airflow too.",
                "Torches burn out after 7 Minecraft days and start dimming after 2. Light is 14 minus (days burning minus 2). Copper torches last twice as long.",
                "Copper lanterns below Y=0 lose 1 light for every 5 blocks below. At Y=-5 the light is 1 lower. At Y=0 and above they stay full.",
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
            "The same food 5 times in the last 7 meals restores less; 7 of the last 7 restores even less. "
            "A meal still gives at least 1 hunger, and saturation does not fall. "
            "The locator bar is off until you eat a golden carrot. It stays on until your saturation is gone. "
            "Another player sees you only while their own bar is on too.",
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
                "Crops grow slowly. Sugar cane is one-tenth speed. Cocoa beans and nether wart are one-twentieth. Trees take 10 times as long, and leaves drop half as many saplings. Each sapling that would still drop has twice the crop loss rate as a percent chance not to, so at 50% loss or higher none drop.",
                "A one-block cane that would grow a second segment may become a weed, using the current crop-loss rate plus the soil under it.",
                "Pumpkins and melons fruit at one-tenth speed. After each fruit the vine may become a weed, starting at 0% and rising 5% per fruit. Broken vines drop no seeds.",
                "Shearing a full hive gives 1 honeycomb.",
                "A bee that returns with nectar fills a hive or nest at 25% of the usual rate. That is farming.beeHoneyPercent. 100 is the usual rate and 0 never fills.",
            ],
            s["bullet"],
        ),
        Paragraph("Soil", s["h"]),
        Paragraph(
            "Unworked soil shows 0. Look at farmland, a crop, or sugar cane to see the plot's modifier. "
            "Green and positive is good. Red and negative is bad. "
            "The first till scores water sources within 2 blocks, up to 30. Past 30, each extra source subtracts 1, down to 0. "
            "A roll of 1 to 10 is then subtracted, and twice the hoe quality is added: wood or stone 0, copper 2, iron 4, diamond 6, gold 8, netherite 10. "
            "A hoe that breaks on that till still counts. "
            "Later hoeing steps the plot toward the hoe's target (wood or stone -10, copper -5, iron 0, diamond +10, gold +20, netherite +30). "
            "Each Efficiency level adds 0.25 to that target's hoe quality: wood or stone 0, copper 1, iron 2, diamond 3, gold 4, netherite 5. "
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
                "Grass spreads onto dirt at a Grass Growth Rate of 25%. That is farming.grassGrowthPercent. 100 is the usual rate and 0 never spreads.",
                "Livestock overgraze by eating grass they can walk to within 6 blocks and turning that block into dirt. About 9 grass blocks keep a large animal fed. About 4 keep a chicken or rabbit fed.",
                "Sheep overgraze too, and wait 6 times as long to eat grass and regrow their wool. farming.sheepWoolGrazeMultiplier of 1 is the usual wool wait.",
                "If no grass block is in reach, an animal has a 33% chance to eat from a chest or barrel it can reach within 20 blocks. It eats short grass, the item shears collect, before its breeding food. If neither is left, a second 33% chance starves it and it drops no meat.",
                "Crowding damages villagers, not livestock. Eight or more in a 3x3x3 take damage. Residents and named villagers are spared. Set farming.overcrowd.villagersOnly false to crowd animals too.",
                "Ordinary villagers take 8 times as long to restock. A novice fletcher buys 32 flint for 1 emerald. That is separate from residents.",
                "An unemployed villager cannot take a job once that job has the rounded-up eighth of the villagers within 64 blocks.",
                "A librarian's enchanted book cannot be a higher level than that librarian. Higher ranks also sell the enchantments from earlier ranks, and can roll them higher. A novice sells Efficiency only at level 1. A master can sell it at level 5.",
                "Composters fill normally but finish slowly. Hopper output from furnaces and composters stores cooking experience in the destination chest at 50% extra.",
            ],
            s["bullet"],
        ),
        Paragraph("Combat", s["h"]),
        bullets(
            [
                "Mobs hit harder. Skeletons ignore arrows. Zombies vary about 20% in speed: faster ones hit softer and look smaller.",
                "Below Y=48, a cave you have walked may spawn an extra hostile. The light may be 11, and every 20 blocks you are below Y=48 adds 1, so Y=28 allows 12 and Y=8 allows 13. The chance falls by 5% times the light of the chosen block.",
                "Below Y=0 in the overworld, normal hostile mobs can spawn in block light of 1 for every 15 blocks down. At Y=-15 that is light 1. At Y=-30 that is light 2.",
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
            "Within 64 blocks, a biome boss shows a health bar with its name. "
            "A brood mother keeps chasing when hit, and every few seconds spits for half her bite damage and a brief blindness. "
            "If a boss cannot walk to you, it breaks blocks in the way at iron-pickaxe speed. "
            "If it is stuck inside a wall, it teleports to the nearest open spot and keeps fighting. "
            "Killing an overworld boss summons a vex, a witch, a skeleton, a creeper, a zombie, a phantom that does not burn in sunlight, and two zombified piglins. "
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
            "Saturation of 18 or more doubles that regen and is not consumed. Below 18 the bonus stops. "
            "A quartz block right-clicked in the air casts Battery. It costs no mana. "
            "Battery lasts 2 minutes per ability level, at least 2 minutes, and multiplies mana regeneration by 1 plus the square root of its level. It renews until your mana reaches your mana level. "
            "Past your level, regeneration is one-quarter speed. "
            "Crystals you will gain in about ten minutes show faintly, the same way they will look. That grows when you are well fed or Battery is on, and shrinks once you are past your mana level. "
            "The old ceiling of 40 mana rises when Well Fed or ready gold pushes your level past 40. "
            "Achievements and wise teachers can raise the permanent mana level. Well Fed adds levels on top, and those extra levels leave when Well Fed drops. "
            "Ready gold adds mana levels too, and those leave when the gold comes off.",
            s["body"],
        ),
        Paragraph("Abilities", s["h"]),
        Paragraph(
            "Hold the catalyst and right-click. Power starts as the square root of how many times you have used that ability. The effective level is at least 1. "
            "A cast consumes one catalyst. Two in hand consume both and add 2. One consumed catalyst adds nothing. "
            "Swords, hoes, and pickaxes are not consumed. "
            "A renew spends no catalyst unless you are holding it, which spends one and adds 2. "
            "Redstone dust right-clicked in the air casts Empower. It lasts 30 seconds per ability level, at least 30 seconds, and does not renew on its own. "
            "While Empower lasts, each later mana ability cast gains extra level equal to the square root of Empower's level. "
            "Gold armor adds 1 mana level after 90 seconds, then 180, 360, and 720 seconds more. Four pieces put on together reach +4 in 1350 seconds. "
            "A golden item in either hand, such as a golden hoe, adds 1 more mana level after 30 seconds. "
            "Gold ingots, nuggets, blocks, and ore do not. The bonus ends the moment that item comes off, and a half-second golden shimmer plus "
            "\"Mana level +1 (+total)\" plays when a piece starts counting. "
            "A golden hoe still keeps its usual Let it grow material bonus on top of that.",
            s["body"],
        ),
        bullets(
            [
                "Paper: Healing. The health is spread across the 6-second blue sparkle. A honey bottle adds +4 and is consumed, including the bottle. Iron ingot: Iron Heart. Feather: Flight. Sneak on a block to stop; sneaking in the air descends. Arrow: Magic arrow. It deals 3 times ability level. Cooldown is 10 seconds minus skill. Every 2 levels, rounded down, another shot goes to a different creature for no extra cost. Charcoal in the hotbar or inventory, not a bundle, adds +2. One charcoal is consumed, and the arrows burn.",
                "Any hoe: Let it grow, on a plant. Coal: Let there be light. Any pickaxe: Power mining. Compass: Detect ore. The compass is kept. Nether quartz is not required. The largest nether quartz stack in the hotbar or inventory, not a bundle, adds the square root of its count, rounded, and that many quartz are consumed from that stack. Reach is 4 + ability level minus that ore's difficulty.",
                "String: Slow. It throws a web that bursts on a creature or a solid block. Spider eye: Sense Evil. Golden sword: Smite Evil. It costs 1/4 mana. With at least 1 mana, the crosshair turns gold on a creature in reach. Clay brick: Master Builder. Diamond: Diamond Skin. Redstone dust in the air: Empower. Quartz block in the air: Battery. Lapis lazuli: Lapis Shield.",
                "Slow flies up to ability level times 2 plus 4 blocks. The burst slows creatures within the ability level of the landing. The slow is 20% plus 5% per level, up to 80%, for 6 seconds per level. Above 100 health, both the slow and the duration are multiplied by 100 divided by that health. It does not affect you, or any player closer to you than the hit.",
                "Battery costs no mana. It lasts 2 minutes per ability level, at least 2 minutes, and multiplies mana regen by 1 plus the square root of its level. It renews until mana reaches your mana level, and ends when it does. A renew consumes one quartz block. Two in hand consume both and add +2.",
                "Lapis lazuli casts Lapis Shield for 1 mana. It lasts 1 minute per ability level. A force field surrounds the targeted character, or you if none is targeted. Damage up to that level is absorbed before armor or health. Every 3 points blocked costs 1 lapis lazuli, rounded up. If you cannot pay, the shield ends and the hit is not absorbed. One lapis lazuli is consumed to cast. Two in hand consume both and add +2.",
                "A book at experience level 7 or higher writes a Tome of the Wise for 1 mana. Above experience level 30 it saves a share of the experience above level 30 and sets you back to level 30. At level 30 or below it saves a share of all your experience and sets you to level 0. That share, on writing and on reading, is 80% plus 2% per ability level, whether you were above 30 or not. Level 10 keeps the whole amount. Below 10 some is lost, and above 10 some is gained. The tome names you. One book makes one tome. Two or more add +2, and each further book makes another tome that splits the saved experience: three, four, or five books make 2, 3, or 4 tomes. More than five still uses five books and makes 4 tomes. Reading costs 1 mana and the tome. Sneak and right-click to open the book instead.",
                "Master Builder lasts 30 seconds times ability level, renews while you have mana, and cancels on another right-click with a clay brick. While it lasts, you can place solid blocks in the air, and block reach increases by the ability level. Torch rules still apply. One brick is consumed. Two in hand consume both and add +2. A renew spends a brick only when you are holding one, and that adds +2.",
                "Diamond Skin costs 2 mana and lasts 30 seconds times ability level. Its effective level is at least 2, so a bare cast still halves health damage. It renews while you have 2 mana, and cancels on another right-click. One diamond is consumed. Two in hand consume both and add +2. A renew spends a diamond only when you are holding one, and that adds +2.",
                "While Diamond Skin lasts, damage that would reach your health is divided by the ability level. Armor still applies first. Each point prevented shortens the effect by 3 seconds. Poison, drowning, and suffocation are not reduced. A quarter-second cyan sparkle plays when a hit is reduced.",
                "You can learn up to half your mana level in abilities, rounded up, by using them. Unlearned use still gains skill, at -3 power, floored at 1, until a slot is free.",
                "Bottom-right icons list abilities above level 2, highest first. Each icon is 5% of normal size times that level, and that level sits on its right at the same scale. Hold a book to see all of them. Otherwise only an active ability, one on cooldown, or the ability for the item in your hand is shown. Cooldown seconds are yellow on the icon. A learned ability's level number is golden.",
                "A wise teacher in a 24-point home teaches one ability for 30 emeralds. Each house point above 24 takes 2 emeralds off, down to free. The lesson ignores the learning cap and does not use a self-learned slot. Paying for an ability you already know frees that slot.",
                "Sneak with a diamond block and a lapis lazuli block and that teacher raises your permanent mana by 1, once per teacher.",
            ],
            s["bullet"],
        ),
        KeepTogether([
            Paragraph("Achievements", s["h"]),
            bullets(
                [
                    "Place many of one block, or defeat many of one monster that drops loot. Each claim gives 50 experience and 4 to 7 emeralds. Everyone can earn every achievement.",
                    "The first player on the server to claim one gets 25 extra experience and 3 more emeralds by default.",
                    "You have your experience level as a percent chance to gain a mana level.",
                    "The first visit to a biome is 50 experience, and 100 more if you are first on the server. The first habitable home in a biome is house points times 3. The first trip 300 blocks from spawn is 100 experience.",
                    "Sneak while holding a block you can place to see how many placements remain. Placing it is not required. A kill always shows that number. It drifts down and left for 3 seconds, and stays hidden when that track is finished.",
                ],
                s["bullet"],
            ),
        ]),
        Paragraph("Homes and residents", s["h"]),
        Paragraph(
            "Right-click a bed with a clock to score a room. An enclosed, lit room of 24 to 300 air blocks, with a bed and a door or fence gate facing outside, "
            "can attract one resident if it has at least 12 furnishing points and stays 20 blocks from another occupied home. "
            "A home more than 50 blocks from the nearest other occupied home gains 1 point, or 2 points when more than 100 blocks away. "
            "Furnishings come from size, extra rooms, windows, art, rugs, seating, storage, workstations, lights, plants, books, a second bed, and a kitchen. "
            "Empty eligible homes are checked at dawn. Chance starts at 8% at 12 points and rises with the score. It is halved in blight. "
            "Chests bias a hauler, a kitchen biases a cook, and plants or workstations bias a farmer. "
            "A cauldron favors a brewer, who can come to a 12-point home. "
            "18 points can bring a drop collector, a wealthy trader, or a jeweler. 24 can bring an armorsmith or a wise teacher. 30 can bring a master armorsmith. "
            "A council member can appear in any eligible home and is especially likely. New residents prefer a specialty that has not appeared yet.",
            s["body"],
        ),
        bullets(
            [
                "A resident will not appear within 35 blocks of a villager. Other residents do not count.",
                "The name over a resident is their name and occupation, such as Alden the Jeweler.",
                "Clothes are a villager outfit, so two of one occupation may look different. A jeweler, wealthy trader, or drop collector wears a finer coat. An armorsmith or wise teacher ranks higher, and a master armorsmith or council member wears the highest rank.",
                "Most residents restock every 14 Minecraft days. Armorsmiths restock every 30. Look at one who is waiting to see the time left, in days and hours.",
                "Shop size follows house points. The top of the roll is 50% at 14 points and 175% at 38. Higher scores stay there. The bottom is half of that. Prices do not inflate. A farm neighbor may sell one seed or plantable vegetable for 6 to 16 emeralds.",
                "A jeweler sells a diamond for 8 emeralds, 4 gold ingots for 2, an iron block for 6, and a copper or redstone block for 2. Larger shops add lapis, amethyst, quartz, gold nuggets, ingots, and blocks of gold, lapis, amethyst, quartz, and diamond. A diamond block is 72 emeralds.",
                "An armorsmith sells one heavy iron piece each restock. Which piece can change. The other sales are enchanted iron armor, each a different slot. A larger shop may buy iron. Heavy iron costs 12, 15, 21, or 24 emeralds for boots, a helmet, leggings, or a chestplate. Enchanted iron costs 6, 8, 11, or 12. A master armorsmith does the same with diamond: one heavy piece at four times the iron price, enchanted diamond armor at the heavy iron price, and a larger shop may buy diamonds.",
                "A brewer sells one level 2 potion each restock. Which potion can change. Up to three other potions cost 1 emerald. A larger shop sells brewing ingredients for 1 emerald, such as nether wart, blaze powder, glowstone, and ghast tears.",
                "Each trade adds 1 to that resident's trading experience. When their stock changes, the square root of that experience is added to the house points for the new shop.",
                "That stock change also rolls item amounts once from -25% to +25% and adds 0 to 25 from the house and trading experience. Buying gives that many more items. Selling to the resident asks for that many fewer. Emerald prices stay the same.",
                "Residents have 40 health plus their house points, and regain 1 health a minute. Looking at one shows hearts, one per 10 health, beside the restock time.",
                "They shoot mobs that come within 12 blocks and are after them. The bow hits 3 harder than a normal arrow. A close attack makes them switch to an iron sword.",
                "A zombie cannot turn a resident unless the hit that kills them starts while they are already below 5 health.",
                "A home stops listing a resident who has died or is no longer around the bed.",
                "A drop collector warns you when a biome boss is out, and names the compass direction from that resident. East is where the sun rises.",
                "Council members each give a personal kill bounty: 6 to 18 common mobs at first, with 14 Minecraft days to finish. Each success with that resident makes their next hunt 20% larger. Every 3 successes, that resident's next hunt is rarer. Another council member keeps a separate ladder, so one can stay easier. You can hold 5 at a time, including ones waiting for emeralds. The next bounty from that resident waits 7 Minecraft days after the last one was given, and only after you finish it.",
                "A kill counts toward every open bounty on that mob. One bounty shows Zombie Bounty 3/12 above the ability icons. Two or more name the resident and stack upward. A bounty that runs out is not replaced in that conversation. The reward is experience from the slain health, plus emeralds when you return to that resident.",
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
