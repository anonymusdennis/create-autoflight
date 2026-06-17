package dev.createautoflight.content.navigation;

import org.joml.Quaterniond;
import org.joml.Vector3d;

public record HoldPositionSnapshot(Vector3d worldPosition, Quaterniond orientation) {}
