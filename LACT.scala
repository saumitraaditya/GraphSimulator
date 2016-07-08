

/**
 * @author sam
 */
import scala.collection.mutable.ArrayBuffer;
import collection.mutable.HashMap
import util.control.Breaks._

class LACT (uid:Int,view:ViewSnapshot,reward_val:Double,deg_constraint:Int)
{
  var myView:ViewSnapshot = view;
  var AutomatonTable = new HashMap[Int,Automaton]();
  for (node_id <- myView.snapshot.keySet)
  {
    AutomatonTable+= node_id->new Automaton(node_id,myView,reward_val,deg_constraint);
  }
  val MaxDeg = deg_constraint;
  var SpanningTree = new ArrayBuffer[edge];
  var CostTree:Double = 0;
  var BestTree:ArrayBuffer[edge] = null;
  var MinTreeCost = Double.MaxValue;
  
  
  def displaySpanningTree(BestSpanningTree:Boolean=false)=
  {
    var displayString = new StringBuilder();
    if (BestSpanningTree)
      for (this_edge<-BestTree)
        displayString.append(" %d-->%d ".format(this_edge.src,this_edge.dst));
    else
      for (this_edge<-SpanningTree)
        displayString.append(" %d-->%d ".format(this_edge.src,this_edge.dst));
    println(displayString);
  }
  /*Iteration results in a degree constrained spanning tree*/
  def start(curr_node_uid:Int):ArrayBuffer[Double]=
  {
    val start_vert = curr_node_uid;
    var current_automaton = AutomatonTable(start_vert);
    SpanningTree = new ArrayBuffer[edge];
    CostTree = 0;
    var verticesInTree = new HashMap[Int,Boolean]() {override def default(key:Int)= false}
    verticesInTree+=(start_vert->true);
    var treeTraceList = new scala.collection.mutable.Stack[Automaton];
    treeTraceList.push(current_automaton);
    var action_probability = new ArrayBuffer[Double]();
    var selectedAction:selected_action = null;
    while (SpanningTree.size != myView.snapshot.keySet.size-1)
      {
        //update action_set to avoid cycle
        current_automaton.cycleAvoidance(verticesInTree);
        if (current_automaton.validateActionSet()==true)
        {
          selectedAction = current_automaton.selectAction();
          SpanningTree+=(selectedAction.sel_edge);
          CostTree = CostTree+selectedAction.sel_cost;
          verticesInTree+=selectedAction.sel_edge.dst->true;
          treeTraceList.push(AutomatonTable(selectedAction.sel_edge.dst))
          action_probability+=selectedAction.action_prob;
        } 
        // if previous automaton is not active or it has no more actions to select
        // trace back for a  active automaton with a non empty action set
        else
        {
          var chosenAutomaton:Automaton = null;
          while (!treeTraceList.isEmpty)
          {
            var candidateAutomaton = treeTraceList.pop();
            candidateAutomaton.cycleAvoidance(verticesInTree);
            if (candidateAutomaton.validateActionSet()==true)
            {
              chosenAutomaton = candidateAutomaton;
              break;
            }
          }
          if (chosenAutomaton == null)
          {
            println("DCST is not possible for this vertex.");
            break;
          }
          
          selectedAction = chosenAutomaton.selectAction();
          SpanningTree+=(selectedAction.sel_edge);
          CostTree = CostTree+selectedAction.sel_cost;
          verticesInTree+=selectedAction.sel_edge.dst->true;
          treeTraceList.push(AutomatonTable(selectedAction.sel_edge.dst))
          action_probability+=selectedAction.action_prob;
        }
        current_automaton = AutomatonTable(selectedAction.sel_edge.dst);
      }
    if (CostTree < MinTreeCost )
      {
        MinTreeCost = CostTree;
        BestTree = SpanningTree;
      }
    // after every iteration all automatons should be refreshed
    for (automatonID <- AutomatonTable.keySet)
    {
      AutomatonTable(automatonID).refresh()
    }
      // returns list of probabilities of selected actions.
    return action_probability;
  }
  
  def iterateTree(curr_vert_ID:Int):ArrayBuffer[edge]=
  {
    var counter = 0;
    while (counter < 100)
    {
      start(curr_vert_ID);
      counter = counter + 1;
    }
    return BestTree;
  }
  
}
