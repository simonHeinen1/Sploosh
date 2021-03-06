package com.htssoft.sploosh;

import java.util.List;

import com.htssoft.sploosh.presentation.FluidTracer;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public class TracerMath {
	/**
	 * Given a list of vortons, compute the field velocity there.
	 * */
	public static void computeVelocityFromVortons(Vector3f position, List<Vorton> influences, 
											  Vector3f store, Vector3f temp1, Vector3f temp2){
		store.zero();
		for (int i = 0; i < influences.size(); i++){
			Vorton v = influences.get(i);
			computeVelocityContribution(position, v, store, temp1, temp2);
		}
		store.multLocal(VortonSpace.ONE_OVER_4_PI);
	}

	/**
	 * Given a vorton, find its influence on the field velocity.
	 * */
	public static void computeVelocityContribution(Vector3f position, Vorton v, Vector3f accum, Vector3f temp1, Vector3f temp2){
		temp2.set(position).subtractLocal(v.getPosition());
		float dist2 = temp2.lengthSquared() + VortonSpace.AVOID_SINGULARITY;
		float oneOverDist = 1f / FastMath.sqrt(dist2);
		float distLaw;
		if (dist2 < VortonSpace.VORTON_RADIUS_SQ){
			distLaw = oneOverDist / VortonSpace.VORTON_RADIUS_SQ;
		}
		else {
			 distLaw = oneOverDist / dist2;
		}
		
		temp1.set(v.getVort()).multLocal(VortonSpace.FOUR_THIRDS_PI * VortonSpace.VORTON_RADIUS_CUBE).crossLocal(temp2).multLocal(distLaw);
		accum.addLocal(temp1);
	}

	public static void moveTracer(FluidTracer tracer, Vector3f fluidVelocity, ThreadVars vars, float tpf){
		Vector3f inertialVel = vars.vec[0];
		Vector3f drag = vars.vec[1];
		Vector3f tempVel = vars.vec[2];

		float step = VortonSpace.DT < tpf ? VortonSpace.DT : tpf;

		drag.set(tracer.inertia);
		drag.subtractLocal(fluidVelocity);

		float sqLen = drag.lengthSquared();

		float F = 0.5f * sqLen * tracer.drag * (FastMath.PI * tracer.radius * tracer.radius);
		drag.negateLocal().normalizeLocal().multLocal(F);
		tempVel.set(drag).multLocal(tpf);
		tracer.inertia.addLocal(tempVel);
		
		inertialVel.set(tracer.inertia);
		
		tempVel.interpolate(inertialVel, fluidVelocity, tracer.reynoldsRatio);
		tempVel.multLocal(tpf);
		tracer.position.addLocal(tempVel);
		
		tracer.age += step;
	}
	
	@SuppressWarnings("unused")
	private static final boolean isValid(float f){
		return !(Float.isInfinite(f) || Float.isNaN(f));
	}
	
	/**
	 * Move a tracer.
	 * */
	public static void advectTracer(FluidTracer tracer, List<Vorton> influences, ThreadVars vars, float currentTPF){
		Vector3f fieldVel = vars.temp0;
		computeVelocityFromVortons(tracer.position, influences, fieldVel, vars.temp1, vars.temp2);
		moveTracer(tracer, fieldVel, vars, currentTPF);
	}

}
