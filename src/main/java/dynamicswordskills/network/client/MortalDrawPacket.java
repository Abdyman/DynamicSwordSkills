/**
    Copyright (C) <2014> <coolAlias>

    This file is part of coolAlias' Zelda Sword Skills Minecraft Mod; as such,
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

package dynamicswordskills.network.client;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import dynamicswordskills.client.DSSClientEvents;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.AbstractMessage;
import dynamicswordskills.skills.ICombo;
import dynamicswordskills.skills.ILockOnTarget;
import dynamicswordskills.skills.MortalDraw;
import dynamicswordskills.skills.SkillBase;

public class MortalDrawPacket extends AbstractMessage
{
	private int swordSlot;

	public MortalDrawPacket() {}

	public MortalDrawPacket(int swordSlot) {
		this.swordSlot = swordSlot;
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		swordSlot = buffer.readInt();
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeInt(swordSlot);
	}

	@Override
	protected boolean isValidOnSide(Side side) {
		return side.isClient();
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		DSSPlayerInfo skills = DSSPlayerInfo.get(player);
		if (skills.hasSkill(SkillBase.mortalDraw)) {
			((MortalDraw) skills.getPlayerSkill(SkillBase.mortalDraw)).drawSword(player, null);
			ILockOnTarget skill = skills.getTargetingSkill();
			if (skill instanceof ICombo) {
				DSSClientEvents.performComboAttack(Minecraft.getMinecraft(), skill);
			}
		}
	}
}
