/**
    Copyright (C) <2016> <coolAlias>

    This file is part of coolAlias' Dynamic Sword Skills Minecraft Mod; as such,
    you can redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software Foundation,
    either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package dynamicswordskills.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.ISkillProvider;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.client.SyncSkillPacket;
import dynamicswordskills.ref.ModInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * Abstract base skill class provides foundation for both passive and active skills
 * 
 */
public abstract class SkillBase
{
	/** Default maximum skill level */
	public static final byte MAX_LEVEL = 5;

	/** For convenience in providing initial id values */
	private static byte skillIndex = 0;

	/**  Map containing all registered skills */
	private static final Map<Byte, SkillBase> skillsMap = new HashMap<Byte, SkillBase>();

	/** List of registered skills' unlocalized names, for use in Commands */
	// if the skillsMap was keyed by unlocalized name, could just return the key set
	private static final List<String> skillNames = new ArrayList<String>();

	public static final SkillBase swordBasic = new SwordBasic("swordbasic").addDescriptions(1);
	public static final SkillBase armorBreak = new ArmorBreak("armorbreak").addDescriptions(1);
	public static final SkillBase dodge = new Dodge("dodge").addDescriptions(1);
	public static final SkillBase leapingBlow = new LeapingBlow("leapingblow").addDescriptions(1);
	public static final SkillBase parry = new Parry("parry").addDescriptions(1);
	public static final SkillBase dash = new Dash("dash").addDescriptions(1);
	public static final SkillBase spinAttack = new SpinAttack("spinattack").addDescriptions(1);
	public static final SkillBase superSpinAttack = new SpinAttack("superspinattack").addDescriptions(1);
	public static final SkillBase mortalDraw = new MortalDraw("mortaldraw").addDescriptions(1);
	public static final SkillBase swordBreak = new SwordBreak("swordbreak").addDescriptions(1);
	public static final SkillBase risingCut = new RisingCut("risingcut").addDescriptions(1);
	public static final SkillBase endingBlow = new EndingBlow("endingblow").addDescriptions(1);
	public static final SkillBase backSlice = new BackSlice("backslice").addDescriptions(1);
	public static final SkillBase swordBeam = new SwordBeam("swordbeam").addDescriptions(1);

	/** Unlocalized name for language registry */
	private final String unlocalizedName;

	/** IDs are determined internally; used as key to retrieve skill instance from skills map */
	private final byte id;

	/** Mutable field storing current level for this instance of SkillBase */
	protected byte level = 0;

	/** Contains descriptions for tooltip display */
	private final List<String> tooltip = new ArrayList<String>();

	/**
	 * Constructs the first instance of a skill and stores it in the skill list
	 * @param name		this is the unlocalized name and should not contain any spaces
	 * @param register	whether to register the skill, adding the skill to the skill list;
	 * 					seems to always be true since skills are declared statically
	 */
	protected SkillBase(String name, boolean register) {
		this.unlocalizedName = name;
		this.id = skillIndex++;
		if (register) {
			if (skillsMap.containsKey(id)) {
				DynamicSwordSkills.logger.warn("CONFLICT @ skill " + id + " id already occupied by "
						+ skillsMap.get(id).unlocalizedName + " while adding " + name);
			}
			skillsMap.put(id, this);
			skillNames.add(unlocalizedName);
		}
	}

	/**
	 * Copy constructor creates a level zero version of the skill
	 */
	protected SkillBase(SkillBase skill) {
		this.unlocalizedName = skill.unlocalizedName;
		this.id = skill.id;
		this.tooltip.addAll(skill.tooltip);
	}

	/** Returns true if the id provided is mapped to a skill */
	public static final boolean doesSkillExist(int id) {
		return (id >= 0 && id <= Byte.MAX_VALUE && skillsMap.containsKey((byte) id));
	}

	/** Returns a new instance of the skill with id, or null if it doesn't exist */
	public static final SkillBase getNewSkillInstance(@Nullable SkillBase skill) {
		return (skill != null && skillsMap.containsKey(skill.getId()) ? skillsMap.get(skill.getId()).newInstance() : null);
	}

	/** Returns the instance of the skill stored in the map if it exists, or null */
	public static final SkillBase getSkill(int id) {
		return (doesSkillExist(id) ? skillsMap.get((byte) id) : null);
	}

	/** Returns an iterable collection of all the skills in the map */
	public static final Collection<SkillBase> getSkills() {
		return Collections.unmodifiableCollection(skillsMap.values());
	}

	/** Returns the total number of registered skills */
	public static final int getNumSkills() {
		return skillsMap.size();
	}

	/** Returns all registered skills' unlocalized names as an array */
	public static final String[] getSkillNames() {
		return skillNames.toArray(new String[skillNames.size()]);
	}

	/**
	 * Retrieves a skill by its unlocalized name, or null if not found
	 */
	public static final SkillBase getSkillByName(String name) {
		for (SkillBase skill : SkillBase.getSkills()) {
			if (name.equals(skill.getUnlocalizedName())) {
				return skill;
			}
		}
		return null;
	}

	/**
	 * Returns a leveled skill from an ISkillProvider using {@link ISkillProvider#getSkillId(ItemStack)}
	 * and {@link ISkillProvider#getSkillLevel(ItemStack)}, or null if not possible
	 */
	public static final SkillBase getSkillFromItem(final ItemStack stack, final ISkillProvider item) {
		SkillBase skill = SkillBase.getSkill(item.getSkillId(stack));
		return createLeveledSkill(skill, item.getSkillLevel(stack));
	}

	/**
	 * Returns a leveled skill from an id and level, capped at the max level for the skill;
	 * May return null if the id is invalid or level is less than 1
	 */
	public static final SkillBase createLeveledSkill(@Nullable final SkillBase skill, final byte level) {
		if (skill != null && level > 0) {
			SkillBase instance = getNewSkillInstance(skill);
			instance.level = (level > skill.getMaxLevel() ? skill.getMaxLevel() : level);
			return instance;
		}
		return null;
	}

	/**
	 * Note that mutable objects such as this are not suitable as Map keys
	 */
	@Override
	public int hashCode() {
		return 31 * (31 + id) + level;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SkillBase skill = (SkillBase) obj;
		return (skill.id == this.id && skill.level == this.level);
	}

	/**
	 * Use this method instead of equals when level is not relevant to the equality comparison
	 * @return true if this skill is the same as another based solely on {@link #getId()}
	 */
	public boolean is(@Nullable SkillBase skill) {
		return (skill != null && this.getId() == skill.getId());
	}

	/** Returns a new instance of the skill with appropriate class type without registering it to the Skill database */
	public abstract SkillBase newInstance();

	/** Returns the translated skill name */
	public final String getDisplayName() {
		return StatCollector.translateToLocal(getFullUnlocalizedName() + ".name");
	}

	/** Returns the string used to translate this skill's name */
	public final String getTranslationString() {
		return getFullUnlocalizedName() + ".name";
	}

	/** Returns the unlocalized name with no prefix, exactly as the skill was registered */
	public final String getUnlocalizedName() {
		return unlocalizedName;
	}

	/** Returns the unlocalized name prefixed by 'skill.dss' */
	public final String getFullUnlocalizedName() {
		return "skill.dss." + unlocalizedName;
	}

	/** Returns texture path for the skill's icon */
	public String getIconTexture() {
		return ModInfo.ID + ":skillorb_" + unlocalizedName;
	}

	/** Each skill's ID can be used as a key to retrieve it from the map */
	public final byte getId() {
		return id;
	}

	/** Returns current skill level */
	public final byte getLevel() {
		return level;
	}

	/** Returns max level this skill can reach; override to change */
	public byte getMaxLevel() {
		return MAX_LEVEL;
	}

	/**
	 * Returns the key used by the language file for getting tooltip description n
	 * Language file should contain key "skill.dss.{unlocalizedName}.desc.{label}.n"
	 * @param label the category for the data, usually "tooltip" or "info"
	 * @param n if less than zero, ".n" will not be appended
	 */
	protected final String getInfoString(String label, int n) {
		return getFullUnlocalizedName() + ".desc." + label + (n < 0 ? "" : ("." + n));
	}

	/** Adds a single untranslated string to the skill's tooltip display */
	protected final SkillBase addDescription(String string) {
		tooltip.add(string);
		return this;
	}

	/** Adds all entries in the provided list to the skill's tooltip display */
	protected final SkillBase addDescription(List<String> list) {
		for (String s : list) { tooltip.add(s); }
		return this;
	}

	/**
	 * Adds n descriptions to the tooltip using the default 'tooltip' label:
	 * {@link SkillBase#getInfoString(String label, int n) getInfoString}
	 * @param n the number of descriptions to add should be at least 1
	 */
	protected final SkillBase addDescriptions(int n) {
		for (int i = 1; i <= n; ++i) {
			tooltip.add(getInfoString("tooltip", i));
		}
		return this;
	}

	/**
	 * Returns the translated tooltip, possibly with advanced display with player information
	 */
	@SideOnly(Side.CLIENT)
	public final List<String> getTranslatedTooltip(EntityPlayer player) {
		List<String> desc = new ArrayList<String>(tooltip.size());
		for (String s : tooltip) {
			desc.add(StatCollector.translateToLocal(s));
		}
		if (Minecraft.getMinecraft().gameSettings.advancedItemTooltips) {
			addInformation(desc, player);
		}
		return desc;
	}

	/** Returns the translated list containing Strings for tooltip display */
	@SideOnly(Side.CLIENT)
	public final List<String> getDescription() {
		List<String> desc = new ArrayList<String>(tooltip.size());
		for (String s : tooltip) {
			desc.add(StatCollector.translateToLocal(s));
		}
		return desc;
	}

	/** Returns a personalized tooltip display containing info about skill at current level */
	@SideOnly(Side.CLIENT)
	public List<String> getDescription(EntityPlayer player) {
		List<String> desc = getDescription();
		addInformation(desc, player);
		return desc;
	}

	/** Allows subclasses to add descriptions of pertinent traits (damage, range, etc.) */
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {}

	/** Returns the translated description of the skill's activation requirements (long version) */
	public String getActivationDisplay() {
		return StatCollector.translateToLocal(getFullUnlocalizedName() + ".desc.activate");
	}

	/** Returns a translated description of the skill's AoE, using the value provided */
	public String getAreaDisplay(double area) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.area", String.format("%.1f", area));
	}

	/** Returns a translated description of the skill's charge time in ticks, using the value provided */
	public String getChargeDisplay(int chargeTime) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.charge", chargeTime);
	}

	/** Returns a translated description of the skill's damage, using the value provided and with "+" if desired */
	public String getDamageDisplay(float damage, boolean displayPlus) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.damage", (displayPlus ? "+" : ""), String.format("%.1f", damage));
	}

	/** Returns a translated description of the skill's damage, using the value provided and with "+" if desired */
	public String getDamageDisplay(int damage, boolean displayPlus) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.damage", (displayPlus ? "+" : ""), damage);
	}

	/** Returns a translated description of the skill's duration, in ticks or seconds, using the value provided */
	public String getDurationDisplay(int duration, boolean inTicks) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.duration", (inTicks ? duration : duration / 20),
				(inTicks ? StatCollector.translateToLocal("skill.dss.ticks") : StatCollector.translateToLocal("skill.dss.seconds")));
	}

	/** Returns a translated description of the skill's exhaustion, using the value provided */
	public String getExhaustionDisplay(float exhaustion) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.exhaustion", String.format("%.2f", exhaustion));
	}

	/** Returns the translated description of the skill's effect (long version) */
	public String getFullDescription() {
		return StatCollector.translateToLocal(getFullUnlocalizedName() + ".desc.full");
	}

	/**
	 * Returns the skill's current level / max level
	 * @param simpleMax whether to replace the numerical display with MAX LEVEL when appropriate
	 */
	public String getLevelDisplay(boolean simpleMax) {
		return ((simpleMax && level == getMaxLevel()) ? StatCollector.translateToLocal("skill.dss.level.max") :
			StatCollector.translateToLocalFormatted("skill.dss.desc.level", level, getMaxLevel()));
	}

	/** Returns a translated description of the skill's range, using the value provided */
	public String getRangeDisplay(double range) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.range", String.format("%.1f", range));
	}

	/** Returns a translated description of the skill's time limit, using the value provided */
	public String getTimeLimitDisplay(int time) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.time", time);
	}

	/** Returns true if player meets requirements to learn this skill at target level */
	protected boolean canIncreaseLevel(EntityPlayer player, int targetLevel) {
		return ((level + 1) == targetLevel && targetLevel <= getMaxLevel());
	}

	/** Called each time a skill's level increases; responsible for everything OTHER than increasing the skill's level: applying any bonuses, handling Xp, etc. */
	protected abstract void levelUp(EntityPlayer player);

	/** Recalculates bonuses, etc. upon player respawn; Override if levelUp does things other than just calculate bonuses! */
	public void validateSkill(EntityPlayer player) {
		levelUp(player);
	}

	/** Shortcut method to grant skill at current level + 1 */
	public final boolean grantSkill(EntityPlayer player) {
		return grantSkill(player, level + 1);
	}

	/**
	 * Attempts to level up the skill to target level, returning true if skill's level increased (not necessarily to the target level)
	 */
	public final boolean grantSkill(EntityPlayer player, int targetLevel) {
		if (targetLevel <= level || targetLevel > getMaxLevel()) {
			return false;
		}
		byte oldLevel = level;
		while (level < targetLevel && canIncreaseLevel(player, level + 1)) {
			++level;
			levelUp(player);
		}
		if (!player.worldObj.isRemote && oldLevel < level) {
			PacketDispatcher.sendTo(new SyncSkillPacket(this), (EntityPlayerMP) player);
		}
		return oldLevel < level;
	}

	/** This method should be called every update tick */
	public void onUpdate(EntityPlayer player) {}

	/**
	 * Write mutable data to NBT.
	 * NOT responsible for storing this skill's identity (e.g. id) - if that is needed, write it separately.
	 */
	public abstract void writeToNBT(NBTTagCompound compound);

	/** Reads mutable data from NBT. */
	public abstract void readFromNBT(NBTTagCompound compound);

	/** Returns a new instance from NBT */
	public abstract SkillBase loadFromNBT(NBTTagCompound compound);

}
