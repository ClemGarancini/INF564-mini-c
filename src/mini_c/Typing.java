package mini_c;

import java.sql.Struct;
import java.util.HashMap;
import java.util.LinkedList;

public class Typing implements Pvisitor {

	// le résultat du typage sera mis dans cette variable
	private File file;

	// et renvoyé par cette fonction
	File getFile() {
		if (file == null)
			throw new Error("typing not yet done!");
		return file;
	}

	// Structures
	HashMap<String, Pstruct> PstructHashMap = new HashMap<String, Pstruct>();
	HashMap<String, Structure> StructuresHashMap = new HashMap<String, Structure>();
	HashMap<String, HashMap<String, Field>> fieldsOfStructuresHashMap = new HashMap<String, HashMap<String, Field>>();
	Structure currentStructure;

	// Functions
	HashMap<String, Decl_fun> functionHashMap = new HashMap<String, Decl_fun>();
	LinkedList<Decl_fun> funs = new LinkedList<Decl_fun>();
	Typ currentFunTyp;

	// Variables and blocs
	LinkedList<HashMap<String,Typ>> currentBlocsVariables = new LinkedList<HashMap<String,Typ>>();

	// Expressions
	Typ currentExprTyp;


	// Outils utiles

	private Typ VariableDeclaredInScope(String id) {
		for (int i = currentBlocsVariables.size() - 1; i >=0 ; i--) {
			Typ type = currentBlocsVariables.get(i).get(id);
			if (type != null) return type;
		}
		return null;
	}

	private boolean isTypeCompatible(Typ type1, Typ type2){
		if (type1 instanceof Ttypenull) {
			return type2 instanceof Tint || type2 instanceof Tstructp || type2 instanceof Ttypenull;
		}
		else if (type1 instanceof Tvoidstar) {
			return type2 instanceof Tstructp || type2 instanceof Tvoidstar;
		}
		else if (type1 instanceof Tstructp) {
			return type2 instanceof Tstructp || type2 instanceof Tvoidstar || type2 instanceof Ttypenull;
		} 
		else {
			return type2 instanceof Tint || type2 instanceof Ttypenull;
		}
	}

	// il faut compléter le visiteur ci-dessous pour réaliser le typage

	@Override
	public void visit(Pfile n) {
		LinkedList<Decl_var> putcharVars = new LinkedList<Decl_var>();
		putcharVars.add(new Decl_var(new Tint(), ""));
		Decl_fun putchar = new Decl_fun(new Tint(), "putchar", putcharVars, null);
		functionHashMap.put("putchar", putchar);

		LinkedList<Decl_var> mallocVars = new LinkedList<Decl_var>();
		mallocVars.add(new Decl_var(new Tint(), ""));
		Decl_fun malloc = new Decl_fun(new Tvoidstar(), "malloc", mallocVars, null);
		functionHashMap.put("malloc", malloc);
		
		for (Pdecl decl : n.l) {
			decl.accept(this);
		}

		if (!functionHashMap.containsKey("main")) {
			throw new Error("Cannot find main function");
		}
		
		File currentFile = new File(funs);
		file = currentFile;
	}

	@Override
	public Tint visit(PTint n) {
		return new Tint();
	}

	@Override
	public Tstructp visit(PTstruct n) {
		if(StructuresHashMap.get(n.id) == null) {
			throw new Error(n.loc + ": " + "Cannot find definition of this structure");
		} else {
			return new Tstructp(StructuresHashMap.get(n.id));
		}
	}

	@Override
	public Econst visit(Pint n) {
		Econst econst = new Econst(n.n);
		if(n.n == 0) {
			econst.typ = new Ttypenull();
		} else {
			econst.typ = new Tint();
		}
		return econst;
	}

	@Override
	public Eaccess_local visit(Pident n) {
		Typ type = this.VariableDeclaredInScope(n.id);
		if ( type != null) {
			Eaccess_local res = new Eaccess_local(n.id); 
			res.typ = type;
			return res;
		}
		throw new Error(n.loc + ": " + "Variable " + n.id + " undefined");
	}

	@Override
	public Eunop visit(Punop n) {
		// We compute the expr e of !e or -e
		Expr e1 = n.e1.accept(this);

		// If the op is - we have to check the tpe of e
		if (n.op == Unop.Uneg && !(this.isTypeCompatible(e1.typ, new Tint()))) {
			throw new Error(n.loc + ": "+ "Illegal syntax: cannot use - with type: " + n.e1.accept(this).typ.toString());
		}
		Eunop eunop = new Eunop(n.op, e1);
		eunop.typ = new Tint();
		return eunop;
	}

	@Override
	public Expr visit(Passign n) {
		// We type the right value of assignment
		Expr e2 =  n.e2.accept(this);

		// Return value will depend on type of assign first expr e1: Pident or Parrow
		if (n.e1 instanceof Pident) {
			// Cast as Pident
			Pident e1AsIdent = (Pident) n.e1;
			Eaccess_local e1 = visit(e1AsIdent);

			//We test if the types are compatible
			if(!this.isTypeCompatible(e2.typ, e1.typ)) {
				throw new Error(n.loc + ": " + "Type mismatch: the assigned and assignee variables must be of same type but they are of type " + e2.typ.toString() + " and " + e1.typ.toString());
			}

			// Create the return value
			Eassign_local eassign_local = new Eassign_local(e1.i, n.e2.accept(this));
			eassign_local.typ = e1.typ;
			return eassign_local;
		} else {
			// Cast as Parrow
			Parrow e1AsArrow = (Parrow) n.e1;
			
			// Create the Eaccess_field object
			Eaccess_field eaccess_field = e1AsArrow.accept(this);
			//We test if the types are compatible
			if(!this.isTypeCompatible(e2.typ, eaccess_field.typ)) {
				System.out.println(e2.typ);
				System.out.println(eaccess_field.typ);
				throw new Error(n.loc + ": " + "Type mismatch: the assigned and assignee variables must be of same type");
			}

			//Create the return value
			Eassign_field eassign_field = new Eassign_field(eaccess_field.e, eaccess_field.f, e2);
			eaccess_field.typ = eaccess_field.typ;
			return eassign_field;
		}
	}

	@Override
	public Ebinop visit(Pbinop n) {
		// We compute the two expressions
		Expr e1 = n.e1.accept(this);
		Expr e2 = n.e2.accept(this);

		// Beq, Bneq, Blt, Ble, Bgt, Bge, Badd, Bsub, Bmul, Bdiv, Band, Bor
		switch (n.op) {
			// If the operator is one among {==,!=,<,<=,>,>=}
			case Beq: case Bneq: case Blt: case Ble: case Bgt: case Bge:
				if (!this.isTypeCompatible(e1.typ, e2.typ)) {
					throw new Error( n.loc + ": "  + "Type mismatch: Binary operators must be used on variables with compatible types");
				}
				break;
			// If the operator is among {+,-,*,/}	
			case Badd: case Bsub: case Bmul: case Bdiv:
				if (!(this.isTypeCompatible(e1.typ, new Tint()) && this.isTypeCompatible(e2.typ, new Tint()))) {
					throw new Error( n.loc + ": "  + "Type mismatch: Binary operators such as {+,-,*,/} must be used on variables with types compatible with int");
				}
				break;
			// If the operator is among {||,&&}
			default:	
				break;
		}
		Ebinop ebinop = new Ebinop(n.op, n.e1.accept(this), n.e2.accept(this));
		ebinop.typ = new Tint();
		return ebinop;

	}

	@Override
	public Eaccess_field visit(Parrow n) {
		// Compute the expre e in e->f
		Expr e = n.e.accept(this);

		//Test if e is a structure
		if (!(e.typ instanceof Tstructp)) {
			throw new Error(n.loc + ": " + "Type mismatch: cannot acces the field of a non-structure variable");
		}
		// Get the corresponding structure
		Tstructp struc = (Tstructp) e.typ;
		
		// Test if the structure is well declared
		if(StructuresHashMap.get(struc.s.str_name) == null) {
			throw new Error(n.loc + ": " + "Structure not yet declared");
		}

		// Get the field f in e->f
		Field field = StructuresHashMap.get(struc.s.str_name).fields.get(n.f);

		// Create the access to field
		Eaccess_field eaccess_field = new Eaccess_field(e, field);
		eaccess_field.typ = field.field_typ;
		return eaccess_field;
	}

	@Override
	public Ecall visit(Pcall n) {
		// We try to get the function from hashmap
		Decl_fun fun = functionHashMap.get(n.f);
		if (fun == null){
			throw new Error(n.loc + " Function " + n.f + " is undefined");
		}
		
		// Get formals parameters
		LinkedList<Decl_var> formals = fun.fun_formals;
		// Create list of effective parameters
		LinkedList<Expr> argumentList = new LinkedList<Expr>();

		// Test if the sizes are the same
		int s1 = formals.size();int s2 = n.l.size();
		if(s1 != s2) throw new Error(n.loc + ": " + String.format("ArgumentError: number of arguments does not match the function's requirements. %d are required but %d were given", s1,s2));

		for (int i = 0; i< s1; i++) {
			Expr expr = n.l.get(i).accept(this);

			// Test if argument types are corresponding
			
			if (!isTypeCompatible(expr.typ, formals.get(i).t))
				throw new Error(n.loc + ": "+ "Type mismatch: arg error in call of function " + fun.fun_name);

			// Add the parameter to the effective list
			argumentList.addLast(expr);
		}
		Ecall ecall = new Ecall(fun.fun_name, argumentList);
		ecall.typ = fun.fun_typ;
		return ecall;
	}

	@Override
	public Esizeof visit(Psizeof n) {
		Structure struct = StructuresHashMap.get(n.id);

		// Test if structure is well defined
		if (struct == null)
			throw new Error(n.loc + ": "+ "Structure " + n.id + " undefined");

		Esizeof esizeof = new Esizeof(struct);
		esizeof.typ = new Tint();
		return esizeof;
	}

	@Override
	public Sskip visit(Pskip n) {
		return new Sskip();

	}

	@Override
	public Stmt visit(Peval n) {
		return new Sexpr(n.e.accept(this));
	}

	@Override
	public Sif visit(Pif n) {
		// Pour if, il suffit de verifier que l'expression e est bien typee ainsi que les instructions s1 et s2
		return new Sif(n.e.accept(this), n.s1.accept(this), n.s2.accept(this));
	}

	@Override
	public Swhile visit(Pwhile n) {
		return new Swhile(n.e.accept(this), n.s1.accept(this));
	}

	@Override
	public Sblock visit(Pbloc n) {
		// We manage the declarations of variables
		LinkedList<Decl_var> dl = new LinkedList<Decl_var>();
		currentBlocsVariables.add(new HashMap<>());
		
		for (Pdeclvar pDeclVar : n.vl) {
			// We check that the Pdeclvar is well-typed
			Decl_var declVar = pDeclVar.accept(this);
			
			// We test if this variable is not already declared in this specific scope
			if (currentBlocsVariables.getLast().containsKey(declVar.name)) {
				throw new Error(n.loc + ": " + "Variable already declared");
			}
			
			//We add the variable to the scope list of defined variables
			currentBlocsVariables.getLast().put(declVar.name, declVar.t);
			
			// We also add it to the list of declVar of function
			dl.add(declVar);
		}
		
		// Then we manage the Stmts
		LinkedList<Stmt> sl = new LinkedList<Stmt>();
		for (Pstmt pstmt : n.sl) {
			Stmt stmt = pstmt.accept(this);
			sl.add(stmt);
		}

		// We remove the current scope from the list of active scopes

		currentBlocsVariables.pollLast();
		return new Sblock(dl, sl);
	}

	@Override
	public Sreturn visit(Preturn n) {
		Expr e  = n.e.accept(this);
		if (!isTypeCompatible(e.typ, currentFunTyp)) {
			throw new Error(n.loc + ": "+ "ReturnError: Function type does not match the return type");
		}
		Sreturn ret = new Sreturn(e);
		return ret;
	}

	@Override
	public Structure visit(Pstruct n) {
		if (PstructHashMap.get(n.s) != null) {
			throw new Error("Structure already defined");
		}

		Structure struct = new Structure(n.s);
		PstructHashMap.put(n.s, n);
		StructuresHashMap.put(struct.str_name, struct);

		HashMap<String, Field> fields = new HashMap<String, Field>();
		
		int field_position = 0;
		for (Pdeclvar decl : n.fl) {
			
			if (fields.get(decl.id) != null){
				throw new Error("Field already defined");
			}

			fields.put(decl.id, new Field(decl.id, decl.typ.accept(this),field_position));
			field_position+=8;
		}
		fieldsOfStructuresHashMap.put(struct.str_name, fields);
		struct.fields = fields;
		struct.size = field_position;
		return struct;
	}

	@Override
	public void visit(Pfun n) {
		LinkedList<Decl_var> fun_formals = new LinkedList<Decl_var>();

		if (functionHashMap.get(n.s) != null) {
			throw new Error(n.loc + ": " + "Function " + n.s + " already defined at: " + n.loc + ": ".toString());
		} else {
			
			currentBlocsVariables.add(new HashMap<>());
			currentFunTyp = n.ty.accept(this);
			Decl_fun fun = new Decl_fun(currentFunTyp, n.s, fun_formals, null);
			functionHashMap.put(fun.fun_name, fun);

			for (Pdeclvar pDeclVar : n.pl) {
				Decl_var declVar = pDeclVar.accept(this);

				if (currentBlocsVariables.getLast().containsKey(declVar.name)) {
					throw new Error(n.loc + ": " + "Variable already declared");
				}
				currentBlocsVariables.getLast().put(pDeclVar.id, pDeclVar.typ.accept(this));
				fun.fun_formals.add(declVar);
			}

			fun.fun_body = n.b.accept(this);

			currentBlocsVariables.pollLast();
			funs.add(fun);
		}
	}

	@Override
	public Decl_var visit(Pdeclvar n) {
		return new Decl_var(n.typ.accept(this), n.id);
	}
}
