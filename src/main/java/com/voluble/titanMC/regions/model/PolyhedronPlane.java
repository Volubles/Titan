package com.voluble.titanMC.regions.model;

public record PolyhedronPlane(double normalX, double normalY, double normalZ, double maximumDotProduct) {

	public PolyhedronPlane {
		if (!Double.isFinite(normalX) || !Double.isFinite(normalY) || !Double.isFinite(normalZ)
				|| !Double.isFinite(maximumDotProduct)) {
			throw new IllegalArgumentException("polyhedron plane values must be finite");
		}
		double lengthSquared = normalX * normalX + normalY * normalY + normalZ * normalZ;
		if (lengthSquared == 0.0D) throw new IllegalArgumentException("polyhedron plane normal must not be zero");
	}

	public boolean contains(int x, int y, int z) {
		return normalX * x + normalY * y + normalZ * z <= maximumDotProduct;
	}
}
