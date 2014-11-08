
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.actor.Props
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

object Main extends App {


  val currentDir = System.getProperty("user.dir")
  val directory = new java.io.File(s"${currentDir}/src/main/resources/blogs")

  val regex = "[0-9]{2}/[0-9]{2}/[0-9]{4}".r

  Actor.controllerActor ! Traverse(directory)
}

class CounterActor extends Actor {
  var matchingWords = 0

  def receive = {
    case Match => {
      matchingWords = matchingWords + 1
    }
    case CurrentTotal => sender ! matchingWords
  }
}

class Controller extends Actor {
  var children:Set[ActorRef] = Set.empty[ActorRef]
  val accumulator = context.actorOf(Props[CounterActor])
  def receive = {
    case Traverse(directory) =>  {
      for {
        dir <- directory.listFiles()
      }
      {
        if(dir.getPath().endsWith(".md")) {
          val readFile = context.actorOf(Props(new ReadFile(dir, accumulator)))
          children += readFile
          readFile ! Read
        }
        else {
          self ! Traverse(dir)
        }

      }
    }
    case Finished => {
      children -= sender
      if(children.size==0) {
        implicit val timeout = new Timeout(1L, TimeUnit.SECONDS)
        val total = accumulator ? CurrentTotal
        total.foreach(t => println(s"total matching words are ${t}"))
      }
    }
  }

}




class ReadFile(file: java.io.File, acc: ActorRef) extends Actor {
  def receive = {
    case Read => {
      for {
        line <- scala.io.Source.fromFile(file).getLines
        word <- line.split(" ")
      }{
        Main.regex.findFirstIn(word).foreach{ _ =>
          acc ! Match
        }
      }
      stop()
    }
  }

  def stop(): Unit = {
    context.parent ! Finished
    context.stop(self)
  }
}


case object Match
case object CurrentTotal
case class Traverse(val directory: java.io.File)
case object Finished
case object Read

object Actor {
  val system = ActorSystem("mySystem")
  val controllerActor = system.actorOf(Props[Controller], "controller")
}

