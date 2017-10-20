/**
 * � Copyright IBM Corporation 2015, 2017.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

import com.urbancode.air.CommandHelper
import com.urbancode.air.plugin.informatica.TextBoxParser


TextBoxParser tBox = new TextBoxParser();

final def out = System.out
final def LS = System.getProperty("line.separator")
final def unique = System.currentTimeMillis()

final def stepProps = new Properties();
final def inputPropsFile = new File(args[0]);
final def inputPropsStream = null;
try {
	inputPropsStream = new FileInputStream(inputPropsFile);
	stepProps.load(inputPropsStream);
}
catch (IOException e) {
	throw new RuntimeException(e);
}

final def srcfolder    = tBox.text2StringArray(stepProps['srcfolder'])
final def tarfolder    = tBox.text2StringArray(stepProps['tarfolder'])
final def commands     = stepProps['commands']
final def srcrepo      = stepProps['srcrepo']
final def srcdomain    = stepProps['srcdomain']
final def srcusername  = stepProps['srcusername']
final def srcpassword  = stepProps['srcpassword']
final def srchost      = stepProps['srchost']
final def srcport      = stepProps['srcport']
final def infaHome = stepProps['infaHome'];
final def srcsecurityDomain = stepProps['srcsecurityDomain']
final def tardomain    = stepProps['tardomain']
final def tarsecurityDomain = stepProps['tarsecurityDomain']
final def tarrepo      = stepProps['tarrepo']
final def tarusername  = stepProps['tarusername']
final def tarpassword  = stepProps['tarpassword']
final def tarhost      = stepProps['tarhost']
final def tarport      = stepProps['tarport']

final def workflowOutputFile = 'informatica_workflowOutput_' + unique + '.in'
final def workflowDependenciesFile = 'informatica_dependencies_' + unique + '.dep'
final def invalidObjectsFile = 'informaticafile_invalidObjects_' + unique + '.invalid'
final def inputFile = 'informatica_script.' + unique + '.in'
final def outputFile = 'informatica_script.' + unique + '.out'

def workflowOutput = new File(workflowOutputFile)
def workflowDependencies = new File(workflowDependenciesFile)
def invalidObjects = new File(invalidObjectsFile)
def workflowList
def controlFile = []
def process

def controlStart = """<!DOCTYPE DEPLOYPARAMS SYSTEM "/u01/app/Dev/Informatica/pwc961hf2/server/bin/depcntl.dtd">
	<DEPLOYPARAMS
	CHECKIN_COMMENTS= \"${new Date().toString()}\"
	COPYPROGRAMINFO = "YES"
	COPYMAPVARPERVALS = "YES"
	COPYWFLOWVARPERVALS = "NO"
	COPYWFLOWSESSLOGS = "NO"
	COPYDEPENDENCY = "YES"
	LATESTVERSIONONLY = "NO"
	RETAINGENERATEDVAL = "YES"
	RETAINSERVERNETVALS = "YES"
	RETAINMAPVARPERVALS = "YES">
  <DEPLOYFOLDER>"""


// If targetfolder  is empty, use source folder names as targetfolder names
  if (!tarfolder) {
	  tarfolder = srcfolder
  }

// Fail if the array sizes are not the same
  else if (tarfolder.size() != srcfolder.size()){
	  throw new Exception ("Enter the same number folders in both Source and Target Informatica Folder properties.")
  }

  for (int i = 0; i < tarfolder.size(); i++) {
	  def filname = "${tarfolder[i]}_" + unique  + i + '.xml'
	  	controlFile += filname
		  File control = new File(filname)
  if (!control.exists()){
	  	control.createNewFile()
  }

  def controlXml = controlStart + '\n' +
			"""<REPLACEFOLDER
    FOLDERNAME=\"${tarfolder[i]}\"
    RETAINMAPVARPERVALS = "YES"
    RETAINWFLOWVARPERVALS = "YES"
    RETAINWFLOWSESSLOGS = "NO"
    MODIFIEDMANUALLY = "NO"
    RETAINORIGFOLDEROWNER = "YES"/> """ + """
	</DEPLOYFOLDER>
	</DEPLOYPARAMS>"""

		control << controlXml
  	}

  	  for (int i = 0; i < controlFile.size(); i++){
	 		File control = new File(controlFile[i])
	  if (control.exists()){
	  
	  		println('controlfile:' + control)

  			}

  		}

	def script = new File(inputFile)

		script << "connect -r $srcrepo -n $srcusername -x $srcpassword "

	if (srcsecurityDomain) {

		script << "-s $srcsecurityDomain "

	}

	if (srcdomain) {

		script << "-d $srcdomain $LS"

	} else {

		script << "-h $srchost -o $srcport $LS"

	}

	for (int i = 0; i < srcfolder.size(); i++) {
		script << "deployfolder -f \"${srcfolder[i]}\"  -c \"${controlFile[i]}\" -r $tarrepo -n $tarusername -x $tarpassword "

	if (tarsecurityDomain) {

		script << "-s $tarsecurityDomain "
	}

	if (tardomain) {

		script << "-d $tardomain $LS"

	} else {

		script << "-h $tarhost -o $tarport $LS"

		}

	}

  	  //println('Deploy folder script content:' + script )
	  //script.eachLine { line -> println(line) }
	  //println('')

	def exitCode = 0
	def command = []
	  if (infaHome != null && infaHome != "") {
		  command.add(infaHome + File.separator + "server" + File.separator + "bin" + File.separator + "pmrep");
	  } else {
	  	  command.add('pmrep')
	  }

	  	  command.add('run')
		  command.add('-o')
		  command.add(outputFile)
		  command.add('-f')
		  command.add(inputFile)
		  command.add('-e')
		  command.add('-s')

		  println('command:' + command )
		  println(command.join(' '))
		  println('')

	def procBuilder = new ProcessBuilder(command as String[]);
		if (infaHome != null && infaHome != "") {
			def env = procBuilder.environment();
				env.put("INFA_HOME", infaHome);

		if (env.get("LD_LIBRARY_PATH") != null && env.get("LD_LIBRARY_PATH") != "") {
				env.put("LD_LIBRARY_PATH", env.get("LD_LIBRARY_PATH") + File.pathSeparator + infaHome + File.separator + "server" + File.separator + "bin");
		} else {
				env.put("LD_LIBRARY_PATH", infaHome + File.separator + "server" + File.separator + "bin");
		}
		
		if (env.get("LIBPATH") != null && env.get("LIBPATH") != "") {
				env.put("LIBPATH", env.get("LIBPATH") + File.pathSeparator + infaHome + File.separator + "server" + File.separator + "bin");
		}else {
				env.put("LIBPATH", infaHome + File.separator + "server" + File.separator + "bin");
		}

			println("With extra  Environment : ");
			println("INFA_HOME : " + env.get("INFA_HOME"));
			println("LD_LIBRARY_PATH : " + env.get("LD_LIBRARY_PATH"));
			println("LIBPATH : " + env.get("LIBPATH"));
		}

			process = procBuilder.start();
			process.consumeProcessOutput(out, out)
			process.getOutputStream().close() // close stdin
			process.waitFor()
			

		def output = new File(outputFile)
			Scanner sc = new Scanner(output)
			println('..........pmrep Deployfolder output:...........')
		def lastLine = ""
			while (sc.hasNextLine()) {
				lastLine = sc.nextLine()
			println(lastLine)
		}
			println('')
			
			sc.close()
		
		
		 so = new Scanner(output)
		
		def failOutList = []
		while (so.hasNextLine()){
			failOutList.add(so.nextLine());
		}
			
		def  execution = failOutList.find{item -> 
							item.contains('Failed to execute deployfolder')}
	
		
		for (int i = 0; i < srcfolder.size(); i++) {
			File control = new File(controlFile[i])
		if (control.exists()){
			control.delete()
			}

		}
		
		script.delete()
		output.delete()
		
		script << "connect -r $tarrepo -n $tarusername -x $tarpassword "

		if (tarsecurityDomain){
			
			script << "-s $tarsecurityDomain "
		}
		if (tardomain) {
			
			script << "-d $tardomain $LS"
			
		} else {
		
			script << "-h $tarhost -o $tarport $LS"
		}

		for (int i = 0; i < tarfolder.size(); i++) {
			
			script << "listobjects -o workflow -f \"${tarfolder[i]}\" $LS"

		}

//		println('list of object script content:')
//		script.eachLine { line -> println(line) }
//		println('')

	
		process = procBuilder.start(); command.execute()
		process.consumeProcessOutput(out, out)
		process.getOutputStream().close() // close stdin
		process.waitFor()

		sc = new Scanner(output)
		//println('..........pmrep list of object output result.......:')
		lastLine = ""
		while (sc.hasNextLine()) {
			lastLine = sc.nextLine()
			//println(lastLine)
		}
			//println('')

		Scanner sa = new Scanner(output)
		
		def list = []
		
		while (sa.hasNextLine()){
			list.add(sa.nextLine());	
		}
			sa.close();
			
		def mapWorkflow = [:]	

		def key = "", workflowitems = []

		for (int i=0; i<list.size(); i++){
			if(list[i].contains('workflow -f')){
				key = list[i].substring(list[i].indexOf('workflow -f')+11,list[i].length() )
				i++
			}
			while(i<list.size() && list[i].startsWith('workflow') && key.size()>0) {
			workflowitems += list[i].minus('workflow').trim()
			i++
			}
			if(key.size()>0 && workflowitems.size()>0){
				mapWorkflow[key.trim()]=workflowitems
				workflowitems =[]
				key=""
			}

		}

   if (mapWorkflow != null && mapWorkflow.size()>0){
			   
		 	for (int i = 0; i < tarfolder.size(); i++) {
				 	def workflowItemList = mapWorkflow.get("\""+tarfolder[i]+"\"")
					 workflowItemList.each{
 
				script <<  "validate -n ${it} -o workflow -f \"${tarfolder[i]}\" -p valid,invalid_after -u $workflowOutput -a  $LS" 
			
					 }

		 	}
				script << "listobjectdependencies -i $workflowOutput -s -p both -u $workflowDependencies -a  $LS"

			if (workflowDependencies){

				script << "validate -i $workflowDependencies -s -k -m validated -p invalid_after  -u $invalidObjects -a $LS"
			
			}
				script << "exit"

//			println('script content-----------:')
//			script.eachLine { line -> println(line) }
//		    println('')
							
		try{

			process = procBuilder.start(); command.execute()
			process.consumeProcessOutput(out, out)
			process.getOutputStream().close()
			process.waitFor()


			sc = new Scanner(output)
			println('-------pmrep validationoutput result----------------:')
			lastLine = ""
			while (sc.hasNextLine()) {
				lastLine = sc.nextLine()
			println(lastLine)
			}
			println('')
		
					
		if (invalidObjects.exists() && invalidObjects.length() > 0){
			
			println "------------------------------------------------------------------------------"
			println "---------------- invalid objects found in the repository.---------------------"
			
			
			
			def invalidlist = []
			    invalidObjects.eachLine{line -> 				
				invalidlist.add(line)			
						
			}
					
				for(line in invalidlist){
					def splitArry = line.split(",")
					def folderNameList = []
					for(int i=2; i<splitArry.size()-2; i++){
						folderNameList.add(splitArry[i])					
							
					}
									
						println("Folder:----" + splitArry[1])
						println("InvalidObjects Found in:------" + folderNameList.join(",") + "n" + " ")
						println("");
						exitCode = 1
						
					}
						
													
				} else if(execution != null){
				
						 println('')
						 println "--------------Migration fail failed to execute deployfolder.-----------"
						 exitCode = 1
			
				} else {
				
						println('')
						println "----------------No objects found in the repository.---------------------"
	   
				}			
				
			output.delete()
			script.delete()
				
			} catch (Exception ex) {
				println "-------- Please review the output log and stack trace for information on the error.---------"
				println ex.printStackTrace()
				exitCode = 1
				
				} finally {
			
				System.exit(exitCode)	
			}

		} else {
				println('')
				println("-------------No invalid objects found in the repository.---------------")
				println('')	
				System.exit(exitCode)
	} 



