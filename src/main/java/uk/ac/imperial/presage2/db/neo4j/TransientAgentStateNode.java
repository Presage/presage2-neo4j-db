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

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.RelationshipIndex;

import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;
import uk.ac.imperial.presage2.db.neo4j.AgentNode.AgentRelationships;

public class TransientAgentStateNode extends NodeDelegate implements
		TransientAgentState {

	public enum TransientAgentStateRel implements RelationshipType {
		AT_TIME, NEXT_STATE
	}

	private final static String INDEX_STATE = "agent_state";
	private final static String KEY_TIME = "time";

	protected TransientAgentStateNode(Node delegate) {
		super(delegate);
	}

	synchronized static TransientAgentStateNode get(AgentNode agent, int time) {
		// check index

		Relationship stateRel = null;
		RelationshipIndex rIndex = agent.getGraphDatabase().index()
				.forRelationships(INDEX_STATE);
		try {
			stateRel = rIndex.get(KEY_TIME, time, agent, null).getSingle();
		} catch (UnsupportedOperationException e) {
			for (Relationship r : agent.getUnderlyingNode().getRelationships(
					AgentRelationships.TRANSIENT_STATE, Direction.OUTGOING)) {
				if (r.getProperty(KEY_TIME).equals(time)) {
					stateRel = r;
					break;
				}
			}
		}

		if (stateRel != null)
			return new TransientAgentStateNode(stateRel.getEndNode());
		else {
			TransientAgentStateNode tsn = null;
			Transaction tx = agent.getGraphDatabase().beginTx();
			try {
				Node n = agent.getGraphDatabase().createNode();
				Relationship fromAgent = agent.createRelationshipTo(n,
						AgentRelationships.TRANSIENT_STATE);
				fromAgent.setProperty(KEY_TIME, time);
				n.createRelationshipTo(
						SimulationTimeNode.get(agent.getGraphDatabase(), time)
								.getUnderlyingNode(),
						TransientAgentStateRel.AT_TIME);
				rIndex.add(fromAgent, KEY_TIME, time);
				tsn = new TransientAgentStateNode(n);
				tx.success();
			} finally {
				tx.finish();
			}
			return tsn;
		}
	}

	void setPrevious(TransientAgentStateNode n) {
		if (!hasRelationship(TransientAgentStateRel.NEXT_STATE,
				Direction.INCOMING)) {
			Transaction tx = getGraphDatabase().beginTx();
			try {
				n.createRelationshipTo(getUnderlyingNode(),
						TransientAgentStateRel.NEXT_STATE);
				tx.success();
			} finally {
				tx.finish();
			}
		}
	}

	@Override
	public int getTime() {
		SimulationTimeNode tn = new SimulationTimeNode(this
				.getSingleRelationship(TransientAgentStateRel.AT_TIME,
						Direction.OUTGOING).getEndNode());
		return tn.getValue();
	}

	@Override
	public PersistentAgent getAgent() {
		PersistentAgent an = new AgentNode(this.getSingleRelationship(
				AgentRelationships.TRANSIENT_STATE, Direction.INCOMING)
				.getStartNode());
		return an;
	}

	@Override
	public void setProperty(String arg0, Object arg1) {
		Transaction tx = this.getGraphDatabase().beginTx();
		try {
			super.setProperty(arg0, arg1);
			tx.success();
		} finally {
			tx.finish();
		}
	}

}
