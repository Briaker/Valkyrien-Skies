package org.valkyrienskies.mod.common.physics.bullet;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.google.common.collect.ImmutableSet;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Value;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.addon.control.block.torque.PhysicsThreadOnly;
import org.valkyrienskies.mod.common.coordinates.ShipTransform;
import org.valkyrienskies.mod.common.physics.IPhysicsEngine;
import org.valkyrienskies.mod.common.physics.bullet.MeshCreator.Triangle;
import org.valkyrienskies.mod.common.physics.management.physo.PhysicsObject;
import org.valkyrienskies.mod.common.physics.management.physo.ShipData;
import org.valkyrienskies.mod.common.physmanagement.shipdata.QueryableShipData;
import org.valkyrienskies.mod.common.util.JOML;

public class BulletPhysicsEngine implements IPhysicsEngine {

    /**
     * Maps the hashcode of a physics object's UUID to it's BulletData. There are two copies of a
     * physics object (client & server side), this ensures that we are only storing data once per
     * physics object.
     */
    private final Map<Integer, BulletData> dataMap = new ConcurrentHashMap<>(); // TIntObjectMap<BulletData> dataMap = new TIntObjectHashMap<>();

    private World mcWorld = null; // please remove this I hate it

    private btDynamicsWorld bulletWorld;
    private btConstraintSolver constraintSolver;
    private btCollisionConfiguration collisionConfig;
    private btCollisionDispatcher collisionDispatcher;
    private btDbvtBroadphase broadphase;

    private final Queue<btRigidBody> queuedToAdd = new ConcurrentLinkedQueue<>();

    public BulletPhysicsEngine() {
        // Note that things are initialized here and not in the object initializer because
        // I don't think it's a good practice to use the object initializer when we're dealing with
        // objects that manually need to be deconstructed
        collisionConfig = new btDefaultCollisionConfiguration();
        broadphase = new btDbvtBroadphase();
        constraintSolver = new btSequentialImpulseConstraintSolver();
        collisionDispatcher = new btCollisionDispatcher(collisionConfig);
        bulletWorld = new btDiscreteDynamicsWorld(collisionDispatcher, broadphase,
            constraintSolver, collisionConfig);
        bulletWorld.setGravity(new Vector3(0f, -9.8f, 0f));

        btGImpactCollisionAlgorithm.registerAlgorithm(collisionDispatcher);
    }

    @Override
    public void addPhysicsObject(@Nonnull PhysicsObject obj) {
        mcWorld = obj.getWorld(); // this is the worst code ever

        ImmutableSet<BlockPos> offsetPos = obj.getBlockPositions().stream()
            .map(pos -> pos.subtract(obj.getReferenceBlockPos()))
            .collect(ImmutableSet.toImmutableSet());


        Vector3dc centerOfMass = obj.getCenterCoord().toVector3d();

        // Create the 'triangle list' from the block positions
        List<Triangle> triangleList = MeshCreator.getMeshTriangles(obj.getBlockPositions(), centerOfMass);



        // Create a mesh from the 'triangle list'
        btTriangleMesh trimesh = MeshCreator.getMesh(triangleList);

        /*
        Vector3 quad[] = new Vector3[]{
                new Vector3(0, 1, -1),
                new Vector3(0, 1, 1),
                new Vector3(0, -1, 1),
                new Vector3(0, -1, -1)};

        trimesh.addTriangle(quad[0], quad[1], quad[2], true);
        trimesh.addTriangle(quad[0], quad[2], quad[3], true);

         */


        btGImpactMeshShape collisionShape = new btGImpactMeshShape(trimesh);
        collisionShape.updateBound(); // This line crashes it
        
        // Create a collision shape that won't crash :/
        // btCollisionShape collisionShape = new btSphereShape(.5f);

        // Generate the construction info for a rigid body from the collision shape and mass
        // float mass = (float) obj.getInertiaData().getGameTickMass();
        float mass = 50;
        btRigidBodyConstructionInfo constructionInfo = new btRigidBodyConstructionInfo(
            mass, null, collisionShape, getLocalInertia(collisionShape, mass));

        // Create a rigid body from the construction info and collision shape
        btRigidBody rigidBody = new btRigidBody(constructionInfo);
        rigidBody.setCollisionShape(collisionShape);
        Matrix4 positionTransform = JOML.toGDX(obj.getTransform().getPositionTransform());
        rigidBody.setWorldTransform(positionTransform);


        queuedToAdd.add(rigidBody);

        // bulletWorld.addRigidBody(rigidBody);

        // Create BulletData and add to dataMap
        BulletData data = new BulletData(triangleList, trimesh, collisionShape, rigidBody);
        dataMap.put(obj.hashCode(), data);
    }

    @Override
    @PhysicsThreadOnly
    public void tick(float delta) {
        if (mcWorld == null) {
            return;
        }
        while (!queuedToAdd.isEmpty()) {
            bulletWorld.addRigidBody(queuedToAdd.remove());
        }
        bulletWorld.stepSimulation(1f/100f);



        dataMap.values().forEach(data ->
            System.out.println(data.getRigidBody().getWorldTransform().getTranslation(new Vector3())));


        /*
        QueryableShipData.get(mcWorld).getShips().stream()
            .filter(shipData -> shipData.getPhyso() != null)
            .map(ShipData::getPhyso)
            .forEach(obj -> {
                BulletData data = getData(obj);
                btRigidBody body = data.rigidBody;
                Matrix4 worldTransform = body.getWorldTransform();
                Matrix4d transform = JOML.convertDouble(worldTransform);
                Vector3d centerCoord = JOML.convert(obj.getCenterCoord());
//                obj.getShipTransformationManager()
//                    .updateAllTransforms(new ShipTransform(transform, centerCoord), false, true);
            });
            
         */
    }

    @Override
    public void unload() {
        bulletWorld.dispose();
    }

    private static Vector3 getLocalInertia(btCollisionShape shape, float mass) {
        Vector3 localInertia = new Vector3();
        shape.calculateLocalInertia(mass, localInertia);
        return localInertia;
    }

    @Nullable
    public BulletData getData(PhysicsObject obj) {
        return dataMap.get(obj.hashCode());
    }

    /**
     * Data used by the Bullet Physics Engine, uniquely assigned to each PhysicsObject
     */
    @Value
    public static class BulletData {

        List<Triangle> triangleList;
        btTriangleMesh trimesh;
        btGImpactMeshShape collisionShape;
        btRigidBody rigidBody;

    }

}