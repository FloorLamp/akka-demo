import akka.actor._

object MapReduce {
  val start: BigInt = 1
  val end: BigInt = 1000000

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("akkaTest")
    val finisher = system.actorOf(Props[Finisher], "finisher")
    val reducer = system.actorOf(Props(new Reducer(finisher)), "reducer")
    val mapper = system.actorOf(Props(new Mapper(reducer)), "mapper")

    (start to end) foreach ( i => mapper ! i )
    println("starting")
  }

  class Finisher extends Actor { 
    def receive = {
      case x =>
        println(s"Result is $x")
        System.exit(0)
    }
  }
   
  class Mapper(reducer: ActorRef) extends Actor {
    def receive = {
      case x: BigInt =>
        reducer ! x*x
        if (x == end) {
          reducer ! "stop"
          context.stop(self)
        }
    }
  }

  class Reducer(finisher: ActorRef) extends Actor {
    var sum: BigInt = 0
    def receive = {
      case x: BigInt =>
        sum += x
      case "stop" =>
        finisher ! sum
        context.stop(self)
    }
  }
}