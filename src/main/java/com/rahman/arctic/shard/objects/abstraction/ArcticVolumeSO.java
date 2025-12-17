package com.rahman.arctic.shard.objects.abstraction;

import lombok.Data;

@Data
public class ArcticVolumeSO {
	private String name;
	private String rangeId;
	private String description;
	private int size;
	private boolean bootable;
	private String imageId;
}