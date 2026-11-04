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
                "Stone and deepslate need an iron pickaxe or better. Breaking coal or ore softens nearby stone. Unsupported stone keeps falling, out to 50 blocks, one step at a time. Dirt and cobble fall the same way.",
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
            "Foods in an inventory or chest, including food inside a bundle or a shop, show a small mark in the top right corner. A black X means you would get very sick of it, and a brown down mark means sick of it. Those follow the same-food penalty, so Well Fed delays them. No mark means you ate it in the last 7 meals. A steel-blue up mark is a slight craving. A green up mark is a craving: it is outside your Well Fed meals, or it is the food that keeps the streak. A gold diamond means it would lengthen a streak of 6 or more different meals. "
            "The locator bar is off until you eat a golden carrot. It stays on until your saturation is gone. "
            "Another player sees you only while their own bar is on too.",
            s["body"],
        ),
        Paragraph("Well Fed", s["h"]),
        Paragraph(
            "After those seven different meals, every two more different meals raise Well Fed by one. "
            "Nine different meals in a row is Well Fed 1. Thirteen is Well Fed 3. It can go higher. "
            "At Well Fed 3 the game looks at your last 16 meals and allows 3 repeats before the level drops. "
            "That grace grows with every level. The same-food penalty also waits one extra repeat per level. "
            "Dying clears the meal history, so Well Fed starts over. "
            "Losing a level removes exactly that level's benefits. Health and hunger clamp down. The mana level leaves, but mana you already hold is kept. That is not an extra hit.",
            s["body"],
        ),
        KeepTogether([
            Paragraph("These leave when Well Fed drops:", s["body"]),
            bullets(
            [
                "+1 health point per level, arriving filled. Odd totals show a half heart outline.",
                "+1 hunger per level, arriving filled. You can keep eating until that higher bar is full. Eating is no longer blocked at 20.",
                "+1 mana level for every 3 Well Fed levels, rounded down. Well Fed 3 is +1. It does not arrive filled, and eating does not restore mana.",
                "+1 melee damage for every 8 Well Fed levels, rounded down. Well Fed 8 is +1.",
                "Food regen is 1 second faster for every 3 Well Fed levels, rounded down, and never faster than once every 5 seconds.",
                "Saturation loss falls 1% per Well Fed level, up to 30%.",
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
                "A ripe cocoa pod drops 1 or 2 cocoa beans. One more bean drops when a d100 is under the hoe quality plus the tree. Quality is iron 0, diamond 10, gold 20, and netherite 30. Each Efficiency level adds 2. Wood, stone, and copper hoes are 0. Each other pod on that tree is -2%. Each jungle log is +2%, up to +20%.",
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
                "Grass spreads onto dirt from farming.grassGrowthPercent. -1, the default, is 75 minus the current crop loss rate, and never below 0. 0 never spreads. A positive number is that percent of the usual spread, and 100 is the usual rate. A fence, gate, door, wall, or other block that leaves the top of the grass open does not stop that spread.",
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
            "A strong boss can appear far from spawn, once for each of 17 biome families. "
            "That boss does not return in that game, including after it is defeated and including in another biome of the same family. "
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
        Paragraph(
            "A game can hold 17 different biome bosses, one for each family. Several biomes share one boss.",
            s["body"],
        ),
        bullets(
            [
                "Lush Cave Broodmother: Lush Caves.",
                "Sulfur Magma Titan: Sulfur Caves.",
                "Dripstone Drowned: Dripstone Caves.",
                "Dark Forest Horde King: Dark Forest and Pale Garden.",
                "Jungle Brood Spider: Jungle, Sparse Jungle, and Bamboo Jungle.",
                "Desert Husk Champion: Desert, Badlands, Eroded Badlands, and Wooded Badlands.",
                "Swamp Hex Witch: Swamp and Mangrove Swamp.",
                "Mushroom Horde King: Mushroom Fields.",
                "Ocean Guardian: Ocean, Cold Ocean, Lukewarm Ocean, Warm Ocean, Frozen Ocean, and all four deep oceans.",
                "Savanna Ravager: Savanna, Savanna Plateau, and Windswept Savanna.",
                "Plains Ravager: Plains, Sunflower Plains, and Meadow.",
                "Cold Stray Sniper: Snowy Plains, Ice Spikes, Taiga, Snowy Taiga, both old-growth taigas, Grove, Snowy Slopes, Frozen Peaks, Jagged Peaks, Stony Peaks, Windswept Hills, and Windswept Gravelly Hills.",
                "River Drowned: River, Frozen River, Beach, Snowy Beach, and Stony Shore.",
                "Forest Broodmother: Forest, Flower Forest, Birch Forest, Old Growth Birch Forest, Cherry Grove, and Windswept Forest.",
                "Nether Blaze Lord: Nether Wastes.",
                "Crimson Hoglin: Crimson Forest.",
                "Soul Sand Wither Knight: Soul Sand Valley.",
            ],
            s["bullet"],
        ),
        Paragraph(
            "These biomes never spawn a biome boss: Deep Dark, Dappled Forest, Warped Forest, Basalt Deltas, The Void, and every End biome.",
            s["body"],
        ),
        Paragraph("Armor", s["h"]),
        Paragraph(
            "A full set of heavy copper, iron, diamond, or netherite armor adds 5, 10, 20, or 30 hearts. "
            "Those pieces last twice as long as the matching vanilla pieces. "
            "Healing and food regen fill every heart, not only the first ten.",
            s["body"],
        ),
        Paragraph("Weapons", s["h"]),
        Paragraph(
            "Heavy swords and axes of copper, iron, gold, and diamond use the same recipe as the normal weapon, "
            "with blocks in place of ingots. They last three times as long and deal 2 more damage. "
            "Repair them with those blocks. Smith the heavy diamond sword or axe with a netherite upgrade template "
            "and a netherite ingot. That ingot repairs the netherite weapon. "
            "A heavy golden sword casts Smite Evil, the same as a golden sword. "
            "A slain biome boss can also drop an enchanted heavy sword or axe. "
            "The chance starts at 25% and rises as that boss is richer.",
            s["body"],
        ),
        Paragraph("Mana", s["h"]),
        Paragraph(
            "Mana is shown as cyan crystals. Two mana points fill one crystal. "
            "Above 3 mana, blue sparkles show for a second or two every 30 to 60 seconds, one for each point of mana. "
            "You regenerate (mana level + current mana) / 200 per minute. "
            "Saturation of 18 or more doubles that regen and is not consumed. Below 18 the bonus stops. "
            "A quartz block right-clicked in the air casts Battery. It costs no mana. "
            "Battery lasts 2 minutes per ability level, at least 2 minutes, and multiplies mana regeneration by 1 plus the square root of its level. It renews until your mana reaches your mana level. "
            "Past your level, regeneration is one-quarter speed. "
            "Crystals you will gain in about ten minutes show faintly, the same way they will look. That grows when you are well fed or Battery is on, and shrinks once you are past your mana level. "
            "The old ceiling of 40 mana rises when Well Fed or ready gold pushes your level past 40. "
            "Achievements and wise teachers can raise the permanent mana level. Well Fed adds one level for every three Well Fed levels, and those extra levels leave when Well Fed drops. "
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
                "Paper: Healing. The health is spread across the 6-second blue sparkle. A honey bottle adds +4 and is consumed, including the bottle. Iron ingot: Iron Heart. Feather: Flight. Sneak on a block to stop; sneaking in the air descends. Arrow: Magic arrow. It deals 3 times ability level. Cooldown is 10 seconds minus skill. Every 3 levels, rounded down, another shot goes to a different creature for no extra cost. Charcoal in the hotbar or inventory, not a bundle, adds +2. One charcoal is consumed, and the arrows burn.",
                "Any hoe: Let it grow, on a plant. Any pickaxe: Power mining. Compass: Detect ore. The compass is kept. Nether quartz is not required. The largest nether quartz stack in the hotbar or inventory, not a bundle, adds the square root of its count, rounded, and that many quartz are consumed from that stack. Reach is your rounded ability level minus that ore's difficulty.",
                "String: Slow. It throws a web that bursts on a creature or a solid block. Spider eye: Sense Evil. Golden sword or heavy golden sword: Smite Evil. It costs 1/4 mana. With at least 1 mana, the crosshair turns gold on a creature in reach. Clay brick: Master Builder. Diamond: Diamond Skin. Redstone dust in the air: Empower. Quartz block in the air: Battery. Lapis lazuli: Lapis Shield.",
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
                "Learning that ability counts as using it once for each house point and each point of his trading experience.",
                "Sneak with a diamond block and a lapis lazuli block and that teacher raises your permanent mana by 1, once per teacher. Each of those levels adds 2 to the achievement die.",
                "Speak to a wise teacher and he tells of a hunt every 30 Minecraft days. The pack is 3 to 8, from the home and his experience, times 1 plus the lessons and mana he has given you.",
                "While he waits to give another hunt, he names up to 3 explored biomes that can still spawn a boss. If more than 3 can, he picks 3 and keeps them until that wait ends.",
                "The first hunts are easier creatures. Later hunts are harder. The hunt lies 200 to 500 blocks north, south, east, or west of that teacher.",
                "His name, how many are left, and the direction show on your screen. That number falls as you kill them. At that distance the direction becomes near here. Come back closer and the direction returns.",
                "After 2 minutes out there, on land, at night or below ground, half the pack appears within a few seconds, 20 to 40 blocks away. The rest still arrive one at a time, every 5 to 15 seconds.",
                "A mark in your line of sight gives off a red mist, so it is not an ordinary creature. A golden carrot mists every mark until you are no longer saturated, and outlines them in white through blocks. Sense Evil does both for 1 minute times its ability level. Each one has 50% to 100% more health. They give triple experience and double loot, and they always drop both. They vanish after 3 Minecraft days.",
                "Sense Evil also outlines every hostile mob within 8 blocks times its ability level. Solid blocks do not hide the outline. It lasts 1 minute times that ability level.",
                "The first creature of a hunt is slightly larger, has 5 times the health, and resists knockback. It gives 10 times the experience and 3 times the loot, plus one diamond and emeralds equal to 10% to 20% of that health. The last creature has twice that health and hits 50% harder. It drops that same loot without the diamond, with emeralds equal to 10% to 20% of its own health.",
                "An unfinished hunt ends after 14 Minecraft days. Each wise teacher can give you a different hunt, and each player has his own.",
            ],
            s["bullet"],
        ),
        KeepTogether([
            Paragraph("Achievements", s["h"]),
            bullets(
                [
                    "Place many of one block, or defeat many of one monster. Each claim gives 75 experience. A three-tier track pays 1 emerald, then 2, then 3. A block that awards once at 64 pays 2 emeralds. Everyone can earn every achievement. Each wool block, wool slab, wool stair, and wool carpet awards once at 64. Glass, tinted glass, stained glass, and glass panes do too. Terracotta, glazed terracotta, concrete, concrete powder, and concrete slabs and stairs do too. Other blocks award at 256, then 512, then 1024.",
                    "The first player on the server to claim one gets 25 extra experience by default.",
                    "achievements.victory defaults to 0, which is off. Set it higher and the first player to earn that many achievements wins. Each builder tier and each slayer tier counts as one. Fireworks play, the credits scroll, and everyone can keep playing. While it is on, the top left shows how many you have earned out of how many are required.",
                    "The command /tougher achieve lists the closest unfinished tracks. A block that has finished its single tier of 64 is left off, including when it is the last block you placed.",
                    "Your experience level is the percent chance of a mana level. It is kept only when a d20, plus 2 for each mana level a wise teacher has given you, rolls above your mana level before Well Fed and gold.",
                    "The first visit to a biome is 50 experience, and 100 more if you are first on the server. The first habitable home of a biome within 250 blocks is house points times 3. Another of that biome farther than 250 blocks pays again. The first trip 300 blocks from spawn is 100 experience.",
                    "Sneak while holding a block you can place to see how many placements remain. Placing it is not required. A kill always shows that number. It drifts down and left for 3 seconds, and stays hidden when that track is finished.",
                ],
                s["bullet"],
            ),
        ]),
        Paragraph("Lapis portals", s["h"]),
        bullets(
            [
                "A door framed in lapis blocks, with a chest on the back wall, is a portal. The floor under the door, the block above it, and the three closed sides are lapis. The side the door faces stays open. Wood and iron doors both open.",
                "The largest stack of lapis lazuli in the chest is the link. A stack of 3 connects only to another booth whose largest stack is also 3. The lapis is not used up.",
                "Stand in the doorway and close the open door. The chest pays in redstone dust, nether quartz, or amethyst shards. One kind covers 100 blocks per item, rounded up, and the cost is at least 1. Two kinds cover 200 blocks per item. All three cover 300.",
                "The kinds already in the chest set that range. Payment alternates across those kinds, largest stack first. When the largest stacks match, nether quartz is first, then redstone dust, then amethyst shards. Each item comes off the largest stack of that kind.",
                "A count shared by more than two booths does not link.",
            ],
            s["bullet"],
        ),
        Paragraph("Homes and residents", s["h"]),
        Paragraph(
            "Right-click a bed with a clock to score a room. An enclosed room of 24 to 400 air blocks, with block light 5 on every floor tile, a bed, and a door or fence gate facing outside, "
            "can attract one resident if it has at least 12 furnishing points and stays 20 blocks from another occupied home. "
            "A home more than 50 blocks from the nearest other occupied home gains 1 point, or 2 points when more than 100 blocks away. "
            "Furnishings come from size, extra rooms, windows, art, rugs, seating, storage, workstations, lights, plants, books, a second bed, and a kitchen. "
            "Empty eligible homes are checked at dawn. Chance starts at 8% at 12 points and rises with the score. It is halved in blight. "
            "Chests bias a hauler, a kitchen biases a cook, and plants or workstations bias a farmer. "
            "A cauldron favors a brewer, who can come to a 12-point home. "
            "18 points can bring a loot collector or a council member. 24 can bring a jeweler, an armorsmith, or a wise teacher. 30 can bring a wealthy trader or a master armorsmith. "
            "A job with more than twice as many residents as the smallest job that home can host is skipped. Wise teachers and council members are skipped only past three times that number. "
            "Council members are especially likely. New residents prefer a specialty that has not appeared yet.",
            s["body"],
        ),
        bullets(
            [
                "A resident will not appear within 35 blocks of a villager. Other residents do not count.",
                "The name over a resident is their name and occupation, such as Alden the Jeweler.",
                "Clock a resident, then clock another. They trade homes at once. Each refuses a home below their job. A loot collector or council member needs 18. A jeweler, armorsmith, or wise teacher needs 24. A wealthy trader or master armorsmith needs 30. Everyone else needs 12. The trade costs half the points of the finer home, rounded down, in emeralds. Their shop stays until the next restock.",
                "Clothes are a villager outfit, so two of one occupation may look different. A jeweler, wealthy trader, or loot collector wears a finer coat. An armorsmith or wise teacher ranks higher, and a master armorsmith or council member wears the highest rank.",
                "Most residents restock every 14 Minecraft days. Armorsmiths restock every 30. Look at one who is waiting to see the time left, in days and hours.",
                "Shop size follows house points. The top of the roll is 50% at 14 points and 175% at 38. Higher scores stay there. The bottom is half of that. Prices do not inflate. A hauler pays 1 emerald for 32 of a block. Each chest, barrel, or shulker box takes 2 off that stack, down to 16.",
                "A farm neighbor may sell one seed or plantable vegetable for 6 to 16 emeralds. Pumpkin and melon seeds are 6, and nether wart is 4. Glow berries are 3 for 1 emerald.",
                "A trader also buys each thing he sells. The buy stack is 2 larger and pays the same number of emeralds. Those buys start with no uses. Buying from him adds 1 use to each of them. Selling him anything spends 1 use on each of them.",
                "A trader or a wealthy trader offers other players' chests after his own trades. Put a sign on a chest, including a trapped chest, whose first matching line is SELL or BUY. A double chest is one shop. Barrels, ender chests, and shulker boxes are not.",
                "The sign can be on the side of the chest, standing on top of it, or hanging under it. Blank lines are skipped. The player who finishes the sign owns the shop, and a new sign on that chest replaces the old one. Your own chest is not offered to you. The chest can be anywhere, including another dimension, and it does not have to stay loaded.",
                "A SELL chest is read from the top left, across each row. Items before the first emeralds are not for sale. An emerald stack sets the price, from 1 to 64, for the items after it. A later emerald stack sets a new price. Emeralds in the bottom two rows do not set a price.",
                "The first stack of an item sets how many he sells in one trade. Later stacks of that same item are extra stock, and he sells those first. They are not a second offer. Emeralds themselves are never sold. Five emeralds, then 16 iron, then 64 more iron, sells 16 iron for 5 emeralds.",
                "The emeralds you pay join emerald stacks in the bottom two rows, starting at the bottom right, then the next stack toward the top left. If those stacks are missing or full, they go in the bottom-right empty slot. He keeps selling while the chest has room for those emeralds. There is no other limit.",
                "A BUY chest is read the same way, but only above the bottom row. An emerald stack there sets the price, and the next new item is the sample: what he buys, and how many. That sample stays in the chest. A later stack of the same item is not another offer.",
                "The bottom row is the wallet. He spends those emeralds from the right. Bought items land on the bottom row first, then on the row above, past the sample. Eight emeralds and then 16 wheat, above that wallet, means he buys 16 wheat for 8 emeralds.",
                "He keeps buying while the wallet can pay and those rows have room. There is no other limit. A trade from another player's chest gives no experience to you or to the resident.",
                "A jeweler sells a diamond for 8 emeralds, 4 gold ingots for 2, an iron block for 6, and a copper or redstone block for 2. Larger shops add lapis, amethyst, quartz, gold nuggets, ingots, and blocks of gold, lapis, amethyst, quartz, and diamond. A gold block is 6 emeralds and a diamond block is 32.",
                "An armorsmith sells one heavy iron piece each restock. Which piece can change. The other sales are enchanted iron armor, each a different slot. A larger shop may buy iron. Heavy iron costs 12, 15, 21, or 24 emeralds for boots, a helmet, leggings, or a chestplate. Enchanted iron costs 6, 8, 11, or 12. A master armorsmith does the same with diamond: one heavy piece at four times the iron price, enchanted diamond armor at the heavy iron price, and a larger shop may buy diamonds.",
                "A brewer sells one level 2 potion each restock. Which potion can change. Up to three other potions cost 1 emerald. A larger shop sells one of each brewing ingredient for 1 emerald, such as nether wart, blaze powder, glowstone, and ghast tears.",
                "A loot collector pays 1 emerald for 4 spider eyes, 4 gunpowder, or 8 bones. A larger shop also pays 1 emerald for 8 rotten flesh, 8 string, 4 slimeballs, 6 leather, 12 arrows, 6 ink sacs, 2 phantom membranes, 2 ender pearls, 2 blaze rods, or 1 ghast tear.",
                "Each trade adds 1 to that resident's trading experience. When their stock changes, the square root of that experience is added to the house points for the new shop.",
                "That stock change also rolls item amounts once from -25% to +25% and adds 0 to 25 from the house and trading experience. Buying gives that many more items. Selling to the resident asks for that many fewer. Emerald prices stay the same.",
                "Residents have 40 health plus their house points, and regain 1 health a minute. Looking at one shows hearts, one per 10 health, beside the restock time.",
                "After a mob hits them, they shoot mobs within 12 blocks that are after them. The bow hits 3 harder than a normal arrow. A close attack makes them switch to an iron sword.",
                "A zombie cannot turn a resident unless the hit that kills them starts while they are already below 5 health.",
                "A home stops listing a resident who has died or is no longer around the bed.",
                "A loot collector warns you when a biome boss is out, and names the compass direction from that resident. East is where the sun rises.",
                "Council members each give a personal kill bounty: 6 to 18 common mobs at first, with 14 Minecraft days to finish. Each success with that resident makes their next hunt 20% larger. Every 3 successes, that resident's next hunt is rarer. Another council member keeps a separate ladder, so one can stay easier. You can hold 5 at a time, including ones waiting for emeralds. The next bounty from that resident waits 7 Minecraft days after the last one was given, plus 3 Minecraft days for each bounty finished with that resident, and only after you finish it.",
                "A kill counts toward every open bounty on that mob. One bounty shows Zombie Bounty 3/12 above the ability icons. Two or more name the resident and stack upward. A bounty that runs out is not replaced in that conversation. The reward is experience from the slain health, plus emeralds when you return to that resident.",
                "At night, each open bounty can send a group within 48 blocks, on or above a solid block dark enough for a monster. Each second of that night, each open bounty has a 1 in 250 chance, so one bounty usually brings 1 or 2 groups. Zero, or 3 or more, is less common. Several open bounties each roll, so more bounties mean more encounters. Each creature stays until the next Minecraft day. The group is a random count from 1/6 of the kills still owed, through 1/3, and at least 1.",
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
