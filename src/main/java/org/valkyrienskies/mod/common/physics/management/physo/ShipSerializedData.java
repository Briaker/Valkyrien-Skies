package org.valkyrienskies.mod.common.physics.management.physo;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.valkyrienskies.mod.common.physmanagement.shipdata.ShipPositionData;

@Data
@Accessors(fluent = false)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true) // For Jackson
public class ShipSerializedData {

    @JsonBackReference
    private final ShipIndexedData indexed;

    /**
     * Physics information -- mutable but final. References to this <strong>should be guaranteed to
     * never change</strong> for the duration of a game.
     */
    private final ShipPhysicsData physicsData;

    /**
     * @deprecated To be replaced by {@link ShipIndexedData#getTransform()}
     */
    @Deprecated
    private ShipPositionData positionData;

    /**
     * Whether or not physics are enabled on this physo
     */
    private boolean physicsEnabled;

}
