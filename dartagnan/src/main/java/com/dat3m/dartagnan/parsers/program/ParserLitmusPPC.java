package com.dat3m.dartagnan.parsers.program;

import com.dat3m.dartagnan.parsers.LitmusPPCLexer;
import com.dat3m.dartagnan.parsers.LitmusPPCParser;
import com.dat3m.dartagnan.exception.ParserErrorListener;
import com.dat3m.dartagnan.parsers.program.utils.ProgramBuilder;
import com.dat3m.dartagnan.parsers.program.visitors.VisitorLitmusPPC;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.Program.SourceLanguage;
import com.dat3m.dartagnan.configuration.Arch;

import org.antlr.v4.runtime.*;

class ParserLitmusPPC implements ParserInterface {

    @Override
    public Program parse(CharStream charStream) {
        LitmusPPCLexer lexer = new LitmusPPCLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        LitmusPPCParser parser = new LitmusPPCParser(tokenStream);
        parser.addErrorListener(new DiagnosticErrorListener(true));
        parser.addErrorListener(new ParserErrorListener());
        ProgramBuilder pb = new ProgramBuilder(SourceLanguage.LITMUS);
        ParserRuleContext parserEntryPoint = parser.main();
        VisitorLitmusPPC visitor = new VisitorLitmusPPC(pb);

        Program program = (Program) parserEntryPoint.accept(visitor);
        program.setArch(Arch.POWER);
        return program;
    }
}
