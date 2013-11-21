import akka.actor._
import akka.routing.RoundRobinRouter
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Future
   
sealed trait Message
case class StartMessage(cmd: String) extends Message
case class DoneMessage extends Message

object TenMillion {
  val numJobs = 10000000
  val numWorkers = 10

  def main(args: Array[String]) {     
    import com.typesafe.config.ConfigFactory
    implicit val system = ActorSystem("TenMillionSystem", ConfigFactory.load.getConfig("tenmillion"))

    val simulator = system.actorOf(
        Props(new SimulateWorkActor(system))
        .withDispatcher("simulator-dispatcher"),
        name="simulator")
 
    val master = system.actorOf(
        Props(new MasterActor(system, numJobs))
        .withDispatcher("simulator-dispatcher"),
        name="master")

    val worker = system.actorOf(
        Props(new WorkerActor(master, simulator))
        .withDispatcher("workers-dispatcher")
        .withRouter(RoundRobinRouter(numWorkers)),
        name="worker")
         
    for (i <- 1 to numJobs) {
      worker ! StartMessage(s"Do job $i")
    }
    println("All jobs sent")
  }
}
 
class WorkerActor(master: ActorRef, simulator: ActorRef) extends Actor {
  import context.dispatcher

  def receive = {
    case msg: StartMessage =>
      implicit val timeout = Timeout(5 minutes)
      val future = simulator ? msg
      future.onComplete {
        case _ =>
          master ! DoneMessage
      }
  }
}
 
class SimulateWorkActor(system: ActorSystem) extends Actor {
  import context.dispatcher

  def receive = {
    case _: StartMessage =>
      system.scheduler.scheduleOnce(1000 milliseconds, sender, 'Done) 
  }
}
 
class MasterActor(system: ActorSystem, numJobs: Int) extends Actor {
  val startedTime = System.currentTimeMillis()
  var count = 0
  def receive = {
    case DoneMessage => 
      count += 1
      if (count%(numJobs/20)==0) println("%d/%d processed".format(count, numJobs))
      if (count == numJobs) {
        val now = System.currentTimeMillis()
        println("Everything processed in %d seconds".format((now-startedTime)/1000))
        system.shutdown() 
      }
  }
}