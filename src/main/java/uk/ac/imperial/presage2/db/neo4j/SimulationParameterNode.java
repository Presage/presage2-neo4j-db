/**
 * 	Copyright (C) 2011 Sam Macbeth <sm1106 [at] imperial [dot] ac [dot] uk>
 *
 * 	This file is part of Presage2.
 *
 *     Presage2 is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Presage2 is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser Public License
 *     along with Presage2.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.imperial.presage2.db.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import uk.ac.imperial.presage2.db.neo4j.Neo4jDatabase.SubRefs;

class SimulationParameterNode extends NodeDelegate {

	enum ParameterRelationships implements RelationshipType {
		PARAMETER_TYPE
	}

	static final String KEY_NAME = "name";

	static SimulationParameterNode get(GraphDatabaseService db,
			String parameterName) {
		Node base = Neo4jDatabase.getSubRefNode(db,
				SubRefs.SIMULATION_PARAMETERS);
		for (Relationship r : base
				.getRelationships(ParameterRelationships.PARAMETER_TYPE)) {
			if (r.getEndNode().getProperty(KEY_NAME).toString()
					.equalsIgnoreCase(parameterName)) {
				return new SimulationParameterNode(r.getEndNode());
			}
		}
		Transaction tx = db.beginTx();
		SimulationParameterNode s = null;
		try {
			Node n = db.createNode();
			n.setProperty(Neo4jDatabase.LABEL, "Property: " + parameterName);
			n.setProperty("name", parameterName);
			base.createRelationshipTo(n, ParameterRelationships.PARAMETER_TYPE);
			s = new SimulationParameterNode(n);
			tx.success();
		} finally {
			tx.finish();
		}
		return s;
	}

	protected SimulationParameterNode(Node delegate) {
		super(delegate);
	}

	public String getName() {
		return (String) this.getProperty(KEY_NAME);
	}

}
