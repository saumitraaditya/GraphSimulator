from graph_tool.all import *
import random
from operator import itemgetter
from collections import defaultdict

class automaton:
    def __init__(self,graph_object,vert_object,reward):
        self.vert=vert_object
        #every automaton maintains an action_set, which is set of incident edges
        #each action is denoted as a triplet
        #<E,probability,valid_for_current_iteration>
        self.edge_costs=graph_object.edge_properties["edge_cost"]
        self.action_set=[]
        self.active=True
        self.numActionsTaken=0
        self.reward=reward
        self.dynamicCost=9999.00 # initial value of minimum Cost incident edge seen till now.
        for e in self.vert.out_edges():
            self.action_set.append([e,1.0/self.vert.out_degree(),True])

    # Edges that connect to a vertex already
    # on the tree should be set to False
    # for the current iteration
    def cycle_avoidance(self,verticesInTree):
        for triplet in self.action_set:
            if (verticesInTree[triplet[0].source()]==True and verticesInTree[triplet[0].target()]==True):
                triplet[2]=False

    # Scale action set if second argument is True else Rescale
    def scaleActionSet(self,scale):
        sum = 0
        for triplet in self.action_set:
            if (triplet[2]==False):
                continue
            sum = sum + triplet[1]
        # set scaled probabilities
        if (scale==True):
            for triplet in self.action_set:
                if (triplet[2] == False):
                    continue
                triplet[1] = triplet[1]/sum
        else:
            for triplet in self.action_set:
                if (triplet[2] == False):
                    continue
                triplet[1] = triplet[1] * sum

    def updateActionSet(self,edge):
        # update performed only for actions that are available
        for triplet in self.action_set:
            if (triplet[0]==edge):
                triplet[1]=triplet[1]+self.reward*(1-triplet[1])
            else:
                if (triplet[2]==True):
                    triplet[1]=(1-self.reward)*triplet[1]

    def validateActionSet(self,degree_constraint,root=False):
        if (root==True):
            if (self.numActionsTaken>degree_constraint):
                self.active=False
                return False
        else:
            if (self.numActionsTaken > degree_constraint - 1):
                self.active=False
                return False
        for action in self.action_set:
            if (action[2]==True):
                return True
        return False

    # Select an action based on the probability distribution
    # if the selected edge is less than dynamic cost of automaton
    # reward the action,update probability and rescale the probabilities
    def selectAction(self):
        self.scaleActionSet(True)
        # sort action set in descending order of probabilities
        self.action_set=sorted(self.action_set,key=itemgetter(1))
        choice=random.random()
        selected_triplet=None
        sum=0
        # print(self.action_set)
        for triplet in self.action_set:
            if (triplet[2]==False):
                continue
            sum=sum+triplet[1]
            if (sum>choice):
                selected_triplet=triplet
                # update probability of this triplet if action is associated with lowest cost edge
                if (self.edge_costs[triplet[0]]<self.dynamicCost):
                    self.updateActionSet(triplet[0])
                    self.dynamicCost=self.edge_costs[triplet[0]]
                break
        self.scaleActionSet(False)
        invited_vertex=None
        if (selected_triplet[0].source()==self.vert):
            invited_vertex=selected_triplet[0].target()
        else:
            invited_vertex=selected_triplet[0].source()
        self.numActionsTaken += 1
        return [invited_vertex,selected_triplet[0],self.edge_costs[selected_triplet[0]]]
    # called our every iteration,
    # resets numActionsTaken, enables all disabled actions
    def refresh(self):
        self.numActionsTaken=0
        for triplet in self.action_set:
            triplet[2]=True

class LACT:
    def __init__(self,graphfile,degree_constraint,reward):
        self.G = load_graph(graphfile)
        # initialize automatons, one at every vertex
        self.AutomatonTable={}
        for v in self.G.vertices():
            self.AutomatonTable[v]=automaton(self.G,v,reward)
        self.MaxDeg=degree_constraint
        self.SpanningTree=[]
        self.CostTree=0

    # Iteration results in a DegreeConstrainedspanningTree
    def start(self):
        start_vert=self.G.vertex(random.SystemRandom().randint(0,self.G.num_vertices()-1))
        print(start_vert)
        current_automaton=self.AutomatonTable[start_vert]
        # reset spanning tree list and cost
        self.SpanningTree = []
        self.CostTree = 0
        verticesInTree=defaultdict(int)
        verticesInTree[start_vert]=1
        while(len(self.SpanningTree)!=self.G.num_vertices()-1):
            # update actionset to avoid cycle
            current_automaton.cycle_avoidance(verticesInTree)
            if (current_automaton.validateActionSet(self.MaxDeg)==True):
                invited_vertex,selected_edge,edge_cost=current_automaton.selectAction()
                self.SpanningTree.append(selected_edge)
                self.CostTree+=edge_cost
                verticesInTree[invited_vertex]=1
            # if previous automaton is not active or it has no more actions to select
            # trace back for a active automaton with a non-empty actions set
            else:
                chosen_vert=None
                for v in verticesInTree.keys():
                    self.AutomatonTable[v].cycle_avoidance(verticesInTree)
                    if (self.AutomatonTable[v].validateActionSet(self.MaxDeg)==True):
                        chosen_vert=v
                        current_automaton=self.AutomatonTable[v]
                        break
                invited_vertex, selected_edge, edge_cost = current_automaton.selectAction()
                self.SpanningTree.append(selected_edge)
                self.CostTree += edge_cost
                verticesInTree[invited_vertex] = 1
            current_automaton = self.AutomatonTable[invited_vertex]
            print(invited_vertex)
        # # print the results from iteration
        # for v in verticesInTree.keys():
        #     print self.G.vertex_index[v]
        for edge in self.SpanningTree:
            print edge

if __name__=="__main__":
    lact = LACT("sample_graph_with_edge_costs.xml.gz",4,.09)
    lact.start()
