/**************************************************************************
 * Copyright (C) OcellO B.V. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Kuan Yan <kuan.yan@ocello.nl>, Jan 12, 2016
 **************************************************************************/
package com.ocello.ky.algorithm.ia.labeling.kernel.labelObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.ocello.ky.algorithm.ia.labeling.kernel.labeledObject.LabeledObject;
import com.ocello.ky.algorithm.ia.labeling.kernel.labeledObject.LabeledObjectFactory;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;


/**
 * Performing child-parent mapping using two binary masks. A parent-object is the container object where child-objects are located inside.
 * For example: the organoid is parent-object for child-object such as nuclei
 *
 *
 * @author Kuan Yan
 * @since Jan 12, 2016
 * @version 1.1.12
 */

public class ObjectMapping
{
	public static void main(String[] args)
	{
		//String ncpath = "\\\\beast\\NAS (Short-Term)\\OcellO Projects\\Synthon\\Extended ADC studies\\SYNE03 GXA3067 Leupeptin\\SYNE03 GXA3067 Leupeptin verification\\1\\C03\\P1_Well_C03_s1_c_DAPI_t1.tif_nc_enhanced_stack_mask.tif";
		//String orgpath = "\\\\beast\\NAS (Short-Term)\\OcellO Projects\\Synthon\\Extended ADC studies\\SYNE03 GXA3067 Leupeptin\\SYNE03 GXA3067 Leupeptin verification\\1\\C03\\P1_Well_C03_s1_c_TRITC_t1.tif_org_stack_mask_filtered.tif";

		String ncpath = "path/a.tif";
		String orgpath = "path/b.tif";

       	ij.Prefs.blackBackground=true;//force black background
       	ij.Prefs.useInvertingLut=false;//force normal lut

       	System.setProperty("plugins.dir", "ImageJ/plugins");//linking ImageJ plugins

		ImagePlus ncbimg = IJ.openImage(ncpath);
		ImagePlus orgbimg = IJ.openImage(orgpath);

		ObjectMapping mapper = new ObjectMapping(ncbimg, orgbimg);
		mapper.getMapping("a.csv");



	}


	private final LabeledObjectFactory nclof, orglof;

	private int width, height, ss;
	private final List<int[][]> orgmaps;

	//constructor
	public ObjectMapping(ImagePlus ncbimg, ImagePlus orgbimg)
	{

		//this.width = orgbimg.getWidth();
		//this.height = orgbimg.getHeight();
		this.ss = orgbimg.getStackSize();

		this.nclof = new LabeledObjectFactory(ncbimg);
		this.orglof = new LabeledObjectFactory(orgbimg);

		this.orgmaps = this.orglof.toMap();
	}


	public boolean getMapping(String outpath)
	{
		ArrayList<Callable<LinkedHashMap<Integer, ArrayList<Integer>>>> workers = new ArrayList<Callable<LinkedHashMap<Integer, ArrayList<Integer>>>>();
		for(int i=0;i<ss;i++)
		{
			workers.add(new InternalWorker(i));
		}

		try
		{
			ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			List<Future<LinkedHashMap<Integer, ArrayList<Integer>>>> res = es.invokeAll(workers);
			es.shutdown();

			ResultsTable rt = new ResultsTable();
			rt.setPrecision(9);
			rt.disableRowLabels();
			rt.showRowNumbers(false);

			LinkedHashMap<Integer, ArrayList<Integer>> match;
			ArrayList<Integer> children;
			LabeledObject org = null, nc = null;
			Future<LinkedHashMap<Integer, ArrayList<Integer>>> re;

			//ImageStack bstack = new ImageStack(width, height);//debug
			for(int sec = 0; sec < res.size(); sec ++)
			{
				re = res.get(sec);
				match = re.get();
				for(int parind : match.keySet())
				{

					//ByteProcessor bip = new ByteProcessor(width,height);
					//ImagePlus bimg = new ImagePlus(sec+"", bip);
					/*
					if(parind>=0)
					{
						org = this.orglof.getObjs(sec).get(parind);
						for(Point p : org.getPoints())
						{
							bimg.setColor(Color.WHITE);
							bimg.getProcessor().drawDot(p.x, p.y);
						}
						IJ.run(bimg, "Outline", "stack");
					}*/

					children = match.get(parind);
					for(int childind: children)
					{
						nc = this.nclof.getObjs(sec).get(childind);

						if(parind >=0)
						{
							org = this.orglof.getObjs(sec).get(parind);
							if(org!=null)
							{
								rt.incrementCounter();
								rt.addValue("par_index", parind);
								rt.addValue("par_mcx", org.getMCPoint().x);
								rt.addValue("par_mcy", org.getMCPoint().y);
								rt.addValue("par_z", sec);
								rt.addValue("child_index", childind);
								rt.addValue("child_mcx", nc.getMCPoint().x);
								rt.addValue("child_mcy", nc.getMCPoint().y);
								rt.addValue("child_z", sec);

								/*
								for(Point p : nc.getPoints())
								{
									bimg.getProcessor().set(p.x, p.y, 127);
								}
								*/
							}
						}
						else//object without parent
						{
							rt.incrementCounter();
							rt.addValue("par_index", -1);
							rt.addValue("par_mcx", -1);
							rt.addValue("par_mcy", -1);
							rt.addValue("par_z", sec);
							rt.addValue("child_index", childind);
							rt.addValue("child_mcx", nc.getMCPoint().x);
							rt.addValue("child_mcy", nc.getMCPoint().y);
							rt.addValue("child_z", sec);
						}

					}
					//bstack.addSlice(bimg.getProcessor());
				}

			}

			//rt.show("map");

			//new ImagePlus("", bstack).show();


			rt.saveAs(outpath);



		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}



 		return true;
	}



	/**syncrhonized access to global storage**/
	private List<LabeledObject> getNCObj(int sec)
	{
		System.out.println("nc:"+sec);
		return nclof.getObjs(sec);
	}
	private List<LabeledObject> getORGObj(int sec)
	{
		System.out.println("org:"+sec);
		return orglof.getObjs(sec);
	}
	private int[][] getObjMap(int sec)
	{
		System.out.println("map:"+sec);
		return orgmaps.get(sec);
	}

	private class InternalWorker implements Callable<LinkedHashMap<Integer, ArrayList<Integer>>>
	{
		private final int _sec;
		public InternalWorker(int sec)
		{
			this._sec = sec;
		}

		/* (non-Javadoc)
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public LinkedHashMap<Integer, ArrayList<Integer>> call() throws Exception
		{
			List<LabeledObject> ncs = getNCObj(_sec);
			List<LabeledObject> orgs = getORGObj(_sec);


			LinkedHashMap<Integer, ArrayList<Integer>> res = new LinkedHashMap<Integer, ArrayList<Integer>>();
			res.put(-1, new ArrayList<Integer>());
			for(int i=0;i<orgs.size();i++)
			{
				res.put(orgs.get(i).getobjnr(), new ArrayList<Integer>());
			}

			int[][] map = getObjMap(_sec);

			LabeledObject nc;
			for(int i = 0; i < ncs.size(); i++)
			{
				nc = ncs.get(i);
				if(nc!=null)
				{
					int orglabel = map[nc.getMCPoint().x][nc.getMCPoint().y]-1;
					res.get(orglabel).add(nc.getobjnr());
				}

			}


			return res;
		}

	}
}
