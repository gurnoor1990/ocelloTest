package com.ocello.ky.algorithm.ia.labeling.kernel.labeledObject;

import java.util.ArrayList;
import java.util.List;

import ij.ImagePlus;

public class LabeledObjectFactory {

	private ImagePlus a;
    private LabeledObject lobject;



	public ImagePlus getA() {
		return a;
	}

	public void setA(ImagePlus a) {
		this.a = a;
	}

	public LabeledObject getLobject() {
		return lobject;
	}

	public void setLobject(LabeledObject lobject) {
		this.lobject = lobject;
	}

	public LabeledObjectFactory(ImagePlus a) {
		super();
		this.a = a;
	}

	public List<int[][]> toMap(){
		List<int[][]> maps = new ArrayList<>();
        ImagePlus ip=this.getA();
        //ip.

		return maps;
	}

	public List<LabeledObject> getObjs(int a){
		List<LabeledObject> b= new ArrayList<LabeledObject>();

		return b;
	}



}
