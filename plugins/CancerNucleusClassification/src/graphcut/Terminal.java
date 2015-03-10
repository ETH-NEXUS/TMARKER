/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package graphcut;

/**
 * The two possible segments, represented as special terminal nodes in the graph.
 */
public enum Terminal {
	FOREGROUND, // a.k.a. the source
	BACKGROUND; // a.k.a. the sink

}
