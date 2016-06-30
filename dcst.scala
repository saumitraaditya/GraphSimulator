import akka.actor.{ActorSystem, ActorRef, Actor, Props,actorRef2Scala,PoisonPill,ActorLogging}
import collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit;
import scala.io.Source


case object start
case object revaluate_socialTopo
// represents a link in the graph.
class edge(source:Int,target:Int,value:Double,trueLink:Boolean)
{
  val src = source;
  val dst = target;
  var cost = value;
  var real = trueLink;
}
/* This class represents unified view of the neighborhood, containing all information 
 * that would be needed by the DCST algorithm to work, each invocation of MinDCST
 * would be provided with a updated snapshot that reflects the network view in the 
 * neighborhood.*/
class ViewSnapshot(socialView:HashMap[Int,ArrayBuffer[Int]],realView:HashMap[Int,ArrayBuffer[Int]])
{
  var snapshot  = new HashMap [Int,ArrayBuffer[edge]]();
  val MaxVal = Double.MaxValue;
  /* initialize snapshot-
   * 1. create storage
   * 2. populate social links
   * 3. identify real subset
   * 4. populate costs*/
  for (source <- socialView.keySet)
  {
    snapshot+=(source->new ArrayBuffer[edge]())
    for (target <- socialView(source))
    {
      snapshot(source)+=new edge(source,target,MaxVal/(socialView(source).size+socialView(target).size),realView(source).contains(target))
    }
  } 
}
// number of actual links to/from the node
case class realDegree(num_links:Int)
// Link advert method
case class LinkAdvt(source:Int, target:Int)
// Exchange Rosters with Friends
case class RosterExchange(sender:Int,roster:ArrayBuffer[Int])
/* A node is an independent network gateway*/
class Node(val uid:Int, var Roster:ArrayBuffer[Int]) extends Actor
{
  /* will contain social links*/
  var socialView = new HashMap[Int,ArrayBuffer[Int]](){ override def default(key:Int) = new ArrayBuffer[Int] }
  /* will contain actual links, needs to be updates when new links are created by self, or information about
   * new link created by neighbors is learnt*/
  var realView = new HashMap[Int,ArrayBuffer[Int]](){ override def default(key:Int) = new ArrayBuffer[Int] }
  /*Initially, node will have edges to to immediate neighbors,
   * gradually it will learn via messaging about edges via neighbor
   * to shared neighbors and add them */
  socialView+=(uid->new ArrayBuffer[Int]());
  realView+=(uid->new ArrayBuffer[Int]());
  for (neighbor <- Roster)
  {
     socialView+=(neighbor->new ArrayBuffer[Int]()); // Adjacency list for neighbors
     socialView(uid)+=neighbor; // Adjacency list for root node.
  }    
  def receive = 
  {
    // Advertisement for real links
    case LinkAdvt(source:Int,target:Int)=>
      {         
            realView(source)+=target;
      }
    case `start`=>
      {
        val Asys = context.system;
        import Asys.dispatcher;
        Asys.scheduler.schedule(new FiniteDuration(1,SECONDS),new FiniteDuration(5,SECONDS),self,revaluate_socialTopo)
      }
    case `revaluate_socialTopo`=>
      {
        /*Simplified implementation -- send my roster to nodes in my roster.*/
        // create a deep copy of Roster and send it across
        val rosterCopy = Roster.clone();
        for (neighbor <- Roster)
        {
          val neighborActor = "../"+neighbor.toString;
          context.actorSelection(neighborActor) ! RosterExchange(uid,rosterCopy)   
        }
      }
    case RosterExchange(sender:Int,neighborRoster:ArrayBuffer[Int])=>
      {
        //Initially only interested in links to shared neighbours.
        // Add to local view a relevant link.
        for (target<-neighborRoster)
        {
          if (socialView.keySet.contains(target))
          {
            socialView(sender)+=target;
            System.out.println("recvd Roster msg");
          }
          
        }
      }
  }
}

class SimulationManager(topology:ArrayBuffer[ActorRef]) extends Actor
{  
  def receive =
  {
    case `start` =>
      {
        for (node <- topology)
        {
          node ! start
        }
      }
  } 
}

object SmartTopology
{
  def main(args:Array[String])
  {
    if (args.length<2)
      println("Please enter the name of Graph file.")
    else
    {
      val GraphFile = args(1);
      val actor_system = ActorSystem("SmartTopology")
      val nodesInNetwork = new ArrayBuffer[ActorRef]();
      // Read the file , initialize the nodes.
      for (line <- Source.fromFile(GraphFile).getLines())
      {
        var roster = new ArrayBuffer[Int]();
        val split_array = line.split(" ");
        val src = split_array(0).toInt;
        for (i<- 1 to split_array.length-1)
          roster+=(split_array(i).toInt)
        nodesInNetwork+=actor_system.actorOf(Props(new Node(src,roster)),src.toString());
      }
      val Manager = actor_system.actorOf(Props(new SimulationManager(nodesInNetwork)),"Manager")
      Manager ! start;
    }
  }
}



