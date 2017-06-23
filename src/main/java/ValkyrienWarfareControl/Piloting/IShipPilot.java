package ValkyrienWarfareControl.Piloting;

import ValkyrienWarfareBase.PhysicsManagement.PhysicsWrapperEntity;
import net.minecraft.util.math.BlockPos;

public interface IShipPilot {

	PhysicsWrapperEntity getPilotedShip();

	boolean isPilotingShip();

	void setPilotedShip(PhysicsWrapperEntity wrapper);

	BlockPos getPosBeingControlled();

	void setPosBeingControlled(BlockPos pos);

	ControllerInputType getControllerInputEnum();

	void setControllerInputEnum(ControllerInputType type);
}
