import java.nio.file.{Path, Paths}

import scopt.{OParserBuilder, Read}

object CommandLineArgs {

  case class Args(
      schemas: Seq[Path] = Seq.empty,
      output: Path = Paths.get(".", "output.yaml")
  )

  implicit val pathRead: Read[Path] = new Read[Path] {
    override def arity: Int = 1

    override def reads: String => Path = Paths.get(_, Array.empty: _*)
  }

  import scopt.OParser

  val builder: OParserBuilder[Args] = OParser.builder[Args]

  val parser: OParser[Unit, Args] = {
    import builder._
    OParser.sequence(
      programName("oapimerge"),
      opt[String]('o', "out file")
        .valueName("<file>")
        .text("Specify output file for merged schema. By default - output.yaml in current working directory. Possible file extensions: yaml, json")
        .action((file, args) => args.copy(output = Paths.get(file))),
      opt[String]('s', "schema")
        .required()
        .unbounded()
        .action((schema, args) => args.copy(schemas = args.schemas.appended(Paths.get(schema))))
        .valueName("<schema>")
        .text("Source openapi schema file"),
      help("help").text("prints this usage text")
    )
  }

  def parse(args: Array[String]): Args =
    OParser.parse(parser, args, Args()) match {
      case Some(config) => config
      case _ =>
        println(OParser.usage(parser))
        sys.exit(1)
    }
}
