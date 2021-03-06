/*-
 * APT - Analysis of Petri Nets and labeled Transition systems
 * Copyright (C) 2012-2013  Members of the project group APT
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package uniol.apt.analysis.connectivity;

import java.util.Iterator;
import java.util.Set;

import uniol.apt.adt.IGraph;
import uniol.apt.adt.INode;
import uniol.apt.module.AbstractModule;
import uniol.apt.module.AptModule;
import uniol.apt.module.Category;
import uniol.apt.module.InterruptibleModule;
import uniol.apt.module.ModuleInput;
import uniol.apt.module.ModuleInputSpec;
import uniol.apt.module.ModuleOutput;
import uniol.apt.module.ModuleOutputSpec;
import uniol.apt.module.exception.ModuleException;

/**
 * Provide the strong connectivity test as a module.
 * @author Uli Schlachter, vsp
 */
@AptModule
public class StrongConnectivityModule extends AbstractModule implements InterruptibleModule {

	@Override
	public String getShortDescription() {
		return "Check if a Petri net or LTS is strongly connected";
	}

	@Override
	public String getName() {
		return "strongly_connected";
	}

	@Override
	public void require(ModuleInputSpec inputSpec) {
		inputSpec.addParameter("graph", IGraph.class, "The graph that should be examined");
	}

	@Override
	public void provide(ModuleOutputSpec outputSpec) {
		outputSpec.addReturnValue("strongly_connected", Boolean.class, ModuleOutputSpec.PROPERTY_SUCCESS);
		outputSpec.addReturnValue("witness_node1", INode.class);
		outputSpec.addReturnValue("witness_node2", INode.class);
	}

	@Override
	public void run(ModuleInput input, ModuleOutput output) throws ModuleException {
		IGraph<?, ?, ?> graph = input.getParameter("graph", IGraph.class);
		Set<? extends Set<? extends INode<?, ?, ?>>> components = run(graph);
		boolean connected = components.size() <= 1;
		output.setReturnValue("strongly_connected", Boolean.class, connected);
		if (!connected) {
			Iterator<? extends Set<? extends INode<?, ?, ?>>> it = components.iterator();
			output.setReturnValue("witness_node1", INode.class, it.next().iterator().next());
			output.setReturnValue("witness_node2", INode.class, it.next().iterator().next());
		}
	}

	@SuppressWarnings("unchecked")
	private static <G extends IGraph<G, ?, N>, N extends INode<G, ?, N>>
			Set<? extends Set<? extends INode<?, ?, ?>>> run(IGraph<?, ?, ?> graph) {
		return Connectivity.getStronglyConnectedComponents((G) graph);
	}

	@Override
	public Category[] getCategories() {
		return new Category[]{Category.PN, Category.LTS};
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
