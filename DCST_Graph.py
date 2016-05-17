from graph_tool.all import *
import random
from operator import itemgetter
from collections import defaultdict
from lact_modified import *
import time

class DCST:
    def __init__(self,graph_file):
        self.G = load_graph(graph_file)
        # print("graph loaded")
        # new edge property, for filtering edges
        edge_isDCST = self.G.new_edge_property("bool", False)
        completed = 0
        for v in self.G.vertices():
            adj_list = self.G.vertex_properties["adj_list"][v]
            lact = LACT(self.G,adj_list,30,.09)
            dcst = lact.iterateTree()
            for e in dcst:
                edge_isDCST[e]=True
            print(lact.MinTreeCost)
            print("-----------------------------------")
            lact.displaySpanningTree(BestTree=True)
            print("-----------------------------------")
            completed+=1
            print(completed)
            print("-----------------------------------")
        self.G.edge_properties["edge_isDCST"]=edge_isDCST
        self.G.save("nx_1000_DCST.xml.gz")
        # start_time = time.time()
        # adj_list = self.G.vertex_properties["adj_list"][self.G.vertex(56)]
        # lact = LACT(self.G, adj_list, 30, .09)
        # dcst = lact.iterateTree()
        # end_time = time.time()
        # print(len(dcst))
        # lact.displaySpanningTree(BestTree=True)
        # print ("Time elapsed %f"%(end_time - start_time))

if __name__== "__main__":
    dcst = DCST("nx_1000_LocalView.xml.gz")
