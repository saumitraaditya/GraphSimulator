import collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import util.control.Breaks._

class ActionSet(action:edge,prob:Double,avbl:Boolean)
{
  val link:edge = action;
  var action_prob = prob;
  var available = avbl;
}
class Automaton (uid:Int,view:ViewSnapshot,reward_val:Double,deg_constraint:Int)
{
  val reward = reward_val;
  val degree_constraint = deg_constraint;
  var dynamicCost= Double.MaxValue;
  var min_initial_action_cost = Double.MaxValue; 
  var sum_costs:Double = 0.0;
  var actionSet = new ArrayBuffer[ActionSet];
  var numActionTaken:Int = 0;
  var active:Boolean = true;
  var costAction = new HashMap[edge,Double]();
  /*calculate minimum cost from the snapshot*/
  for (link <- view.snapshot(uid))
  {
    sum_costs = sum_costs + link.cost;
    if (link.cost <= min_initial_action_cost)
    {
      min_initial_action_cost = link.cost;
    }
  }
  /* Initialize action set*/
  for (link <- view.snapshot(uid))
  {
    assignCosts(link);
    actionSet+=(new ActionSet(link,costAction(link),true))
  }
  
  def assignCosts(link:edge)=
  {
    if (link.real)
    {
      costAction+=(link->scala.util.Random.nextDouble*min_initial_action_cost/2);
    }
    else
    {
      var excess= 0;
      // filter edges that are real, find sum-- for both source and dst of the link.
      excess = excess+view.snapshot(link.src).filter(_.real==true).size
      excess = excess+view.snapshot(link.dst).filter(_.real==true).size
      costAction+=(link->(1+excess/degree_constraint)*link.cost)
    }
  }
  
  def scaleActionSet(scale:Boolean=true):Double=
  {
    var sum:Double = 0;
    for (action <-actionSet)
    {
      if (action.available)
      {
        sum = sum + action.action_prob;
      }
    }
    if (scale==true)
    {
      for (action <-actionSet)
      {
        if (action.available)
          action.action_prob = action.action_prob/sum;
      }
    }
    else
    {
      for (action <-actionSet)
      {
        if (action.available)
          action.action_prob = action.action_prob * sum;
      }
    }
    return sum;
  }
  
  def updateActionSet(chosen_action:ActionSet,to_reward:Boolean=true)
  {
    if (to_reward)
    {
      for (action <-actionSet)
      {
        if (action == chosen_action)
          action.action_prob = action.action_prob + reward * (1 - action.action_prob);
      }
    }
    else
    {
      for (action <-actionSet)
      {
        if (action == chosen_action)
          action.action_prob = action.action_prob  * (1 - reward);
      }
    }
  }
  
  def validateActionSet(root:Boolean=false):Boolean=
  {
    if (root && numActionTaken >= degree_constraint)
    {
        active = false;
        return false;    
    }
    else if (numActionTaken >= degree_constraint-1)
    {
      active = false;
      return false;
    }
    else
    {
      for (action <- actionSet)
        if (action.available)
          return true
      return false;
    }
  }
  
  def cycleAvoidance(inTree:HashMap[Int,Boolean])
  {
    for (action<-actionSet)
    {
      if (inTree(action.link.src) && inTree(action.link.dst))
        action.available = false;
    }
  }
  
  def selectAction()
  {
    val action_prob_sum = scaleActionSet(true);
    // sort action set in descending order of probabilities
    actionSet = actionSet.sortWith(_.action_prob > _.action_prob);
    var choice = scala.util.Random.nextDouble();
    var selectedAction:ActionSet = null;
    var sum:Double = 0.0;
    for (action <- actionSet)
    {
      if (action.available)
      {
        sum = sum + action.action_prob;
        if (sum >= choice)
        {
          selectedAction = action;
          break;
        }
      }
    }
    
    
    
  }
  
}
