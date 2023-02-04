<!DOCTYPE html>
<html>
	<head>
		<meta charset='utf-8'>
		<meta name='viewport' content='width=device-width, initial-scale=1'>
		<meta name='description' content='test'>
		<meta name='theme-color' content='#ffcd00'>
		<meta property='og:description' content='test'>
		<title>test</title>

		<link rel='stylesheet' href="/css/custom.css">
		<!-- single quotes -->
		<link rel='stylesheet' href='/css/custom.css'>
		<!-- multiline -->
		<link 
		rel='stylesheet' 
		href='/css/custom.css'
		>
		
		<script type="text/javascript" src="/js/script.js"></script>
		<!-- single quotes -->
		<script type="text/javascript" src='/js/script.js'></script>
		<!-- multiline -->
		<script 
		type="text/javascript" 
		src='/js/script.js'
		></script>
		
	</head>
	<body class='home type-page has-sidebar'>
		<img class="someclass" src="/img/img.png" />
		<!-- single quotes -->
		<img class="someclass" src='/img/img.png' />
		<!-- multiline -->
		<img 
		src="/img/img.png"
		class="someclass"  
		/>
		<!-- relative urls will be ignored -->
		<img class="someclass" src="relative.png"/>
		<!-- ignored -->
		<img class="someclass" src="data:image/gif;base64,R0lGODlhEAAOALMAAOazToeHh0tLS/7LZv/0jvb29t/f3//Ub//ge8WSLf/rhf/3kdbW1mxsbP//mf///yH5BAAAAAAALAAAAAAQAA4AAARe8L1Ekyky67QZ1hLnjM5UUde0ECwLJoExKcppV0aCcGCmTIHEIUEqjgaORCMxIC6e0CcguWw6aFjsVMkkIr7g77ZKPJjPZqIyd7sJAgVGoEGv2xsBxqNgYPj/gAwXEQA7"/>
		
		<!-- default type='text' should be removed -->
		<input class="someclass" type="text" name="test" value="test"/>
		<!-- pre should be ignored -->
		<pre>
			Preformatted
		</pre>
	</body>
</html>