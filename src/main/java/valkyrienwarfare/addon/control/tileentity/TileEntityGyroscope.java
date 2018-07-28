/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2015-2018 the Valkyrien Warfare team
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income unless it is to be used as a part of a larger project (IE: "modpacks"), nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from the Valkyrien Warfare team.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: The Valkyrien Warfare team), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package valkyrienwarfare.addon.control.tileentity;

import gigaherz.graph.api.GraphObject;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import valkyrienwarfare.addon.control.nodenetwork.BasicNodeTileEntity;
import valkyrienwarfare.addon.control.nodenetwork.VWNode_TileEntity;
import valkyrienwarfare.api.TransformType;
import valkyrienwarfare.math.Vector;
import valkyrienwarfare.physics.PhysicsCalculations;

public class TileEntityGyroscope extends TileEntity {

	private static final Vector GRAVITY_UP = new Vector(0, 1, 0);
	private double torquePower = 10000;
	
	public Vector getTorqueInGlobal(PhysicsCalculations physicsCalculations, BlockPos pos) {
		Vector shipLevelNormal = new Vector(GRAVITY_UP);
		physicsCalculations.getParent().getShipTransformationManager().getCurrentPhysicsTransform().rotate(shipLevelNormal, TransformType.SUBSPACE_TO_GLOBAL);
		Vector torqueDir = GRAVITY_UP.cross(shipLevelNormal);
		double angleBetween = GRAVITY_UP.angleBetween(shipLevelNormal);
		torqueDir.normalize();
		torqueDir.multiply(physicsCalculations.getPhysicsTimeDeltaPerPhysTick() * torquePower * angleBetween * -100);
		return torqueDir;
	}

}
