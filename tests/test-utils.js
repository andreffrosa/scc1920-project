'use strict';

/***
 * Exported functions to be used in the testing scripts.
 */
module.exports = {
  genNewUser,
  genNewUserReply,
  genNewCommunity,
  genNewPost,
  genNewPostReply,
  setNewPostImageBody,
  genNewImageReply,
  getImageReply,
  hasMoreInBrowseList,
  hasMoreInImageList,
  selectAllFromPostList,
  selectFromPostList,
  selectFromPostThread,
  startBrowse,
  endBrowse
}


const Faker = require('faker')
const fs = require('fs')
const fetch = require('node-fetch')

var userNames = [] 
var userIds = []
var communityNames = []
var postIds = []
var images = []

// All endpoints starting with the following prefixes will be aggregated in the same for the statistics
var statsPrefix = [ ["/post/thread/","GET"],
	["/post/p/like/","POST"],
	["/post/p/unlike/","POST"],
	["/image/id","GET"],
	["/post/p/","GET"],
	["/users/","GET"],
	["/community/pt","GET"],
    ["/media/","GET"]
	]

// Function used to compress statistics
global.myProcessEndpoint = function( str, method) {
	var i = 0;
	for( i = 0; i < statsPrefix.length; i++) {
		if( str.startsWith( statsPrefix[i][0]) && method == statsPrefix[i][1])
			return method + ":" + statsPrefix[i][0];
	}
	return method + ":" + str;
}

// Auxiliary function to select an element from an array
Array.prototype.sample = function(){
	   return this[Math.floor(Math.random()*this.length)]
}

// Returns a random value, from 0 to val
function random( val){
	return Math.floor(Math.random() * val)
}

// Loads data about users, communities and images from disk
// This data is written when users and communities are saved
function loadData() {
	if( userNames.length > 0)
		return;
	var str;
	if( fs.existsSync('usernames.data')) {
		str = fs.readFileSync('usernames.data','utf8')
		userNames = JSON.parse(str)
	} 
	if( fs.existsSync('userids.data')) {
		str = fs.readFileSync('userids.data','utf8')
		userIds = JSON.parse(str)
	}
	if( fs.existsSync('communitynames.data')) {
		str = fs.readFileSync('communitynames.data','utf8')
		communityNames = JSON.parse(str)
	}
	var i
	var basefile
	if( fs.existsSync( '/images')) 
		basefile = '/images/cats.'
	else
		basefile =  'images/cats.'	
	for( i = 1; i <= 40 ; i++) {
		var img  = fs.readFileSync(basefile + i + '.jpeg')
		images.push( img)
	}
}

loadData();


/**
 * Generate data for a new user using Faker
 */
function genNewUser(context, events, done) {
	const name = `${Faker.name.firstName()}.${Faker.name.lastName()}`
	context.vars.name = name
	userNames.push(name)
	fs.writeFileSync('usernames.data', JSON.stringify(userNames))
	return done()
}


/**
 * Process reply for of new users to store the id on file
 */
function genNewUserReply(requestParams, response, context, ee, next) {
	if( response.status >= 200 && response.status < 300 && response.body.length > 0) {
		userIds.push(response.body)
		fs.writeFileSync('userids.data', JSON.stringify(userIds));
	}
    return next()
}


/**
 * Generate data for a new community using Faker
 */
function genNewCommunity(context, events, done) {
	const name = `s/${Faker.lorem.word()}`;
	context.vars.name = name;
	communityNames.push(name);
	fs.writeFileSync('communitynames.data', JSON.stringify(communityNames));
	return done()
}


/**
 * Generate data for a new post. Starts by loading data if it was not loaded yet.
 * Stores in the variables:
 * "cammunity" : the name of a community
 * "creator" : the name of a user
 * "msg" : the contents for the message
 * "parentId" : the identifier of a post, so that this post is a reply to that
 * "hasImage" : true/false, depending on whether there will be an image in the post
 * "image" : the contents of the selected image
 */
function genNewPost(context, events, done) {
	loadData();
	context.vars.community = communityNames.sample()
	context.vars.creator = userNames.sample()
	context.vars.msg = `${Faker.lorem.paragraph()}`;
	if( postIds.length > 0 && Math.random() < 0.8) {  // 80% are replies
		let npost = postIds.sample()
		context.vars.parentId = npost[0]
		context.vars.community = npost[1]
	} else {
		context.vars.parentId = null
	}
	context.vars.hasImage = false 
	if(Math.random() < 0.2) {   // 20% of the posts have images
		context.vars.image = images.sample()
		context.vars.hasImage = true 
	}
	return done()
}

/**
 * Select next post to read and store information in the following variables:
 * "nextid" - id of the next post to browse
 * "hasNextid" - true/false whether there is any other post to browse next
 * "browsecount" - update information on the session size

 */
function hasMoreInBrowseList(context, next) {
	if( context.vars.idstoread.length > 0) {
		let pp = context.vars.idstoread.splice(-1,1)[0]
		context.vars.nextid = pp[0]
		context.vars.nextcommunity = pp[1]
	    context.vars.hasNextid = true
	    context.vars.browsecount++
	} else {
		context.vars.hasNextid = false
	}
	return next(context.vars.hasNextid)
}

/**
 * Function that checks if there are more images to load. Axuiliary function.
 * Store information in the following variables:
 * "nextid" - id of the next post to browse
 * "hasNextid" - true/false whether there is any other post to browse next
 */
function checkHasMoreInImageList(context) {
	context.vars.hasNextimageid = false
	while( context.vars.hasNextimageid == false && context.vars.postlistimages.length > 0) {
		context.vars.nextimageid = context.vars.postlistimages.splice(-1,1)[0] // remove element from array
	    context.vars.hasNextimageid = ! context.vars.readimages.has(context.vars.nextimageid)
	}
}
function hasMoreInImageList(context, next) {
	checkHasMoreInImageList(context)
	return next(context.vars.hasNextimageid)
}

/**
 * Process reply of the new post. 
 * Stores in the array "postIds" the identifier and community of the uploaded post.
 */
function genNewPostReply(requestParams, response, context, ee, next) {
	if( response.body && response.body.length > 0) {
		postIds.push([response.body,context.vars.community])
	}
    return next()
}

/**
 * Sets the body to an image, when using images.
 */
function setNewPostImageBody(requestParams, context, ee, next) {
	if( context.vars.hasImage)  {
		requestParams.body = context.vars.image
	}
	return next()
}

/**
 * Process reply of the upload of an image. 
 * Stores in the variable "imageId" the identifier of the uploaded image.
 */
function genNewImageReply(requestParams, response, context, ee, next) {
	if( response.body && response.body.length > 0) {
		context.vars.imageId = response.body
	}
    return next()
}

/**
 * Process reply of the download of an image. 
 * Update the next image to read.
 */
function getImageReply(requestParams, response, context, ee, next) {
	if( typeof response.body !== 'undefined' && response.body.length > 0) {
		context.vars.readimages.add(context.vars.nextimageid)
	}
	checkHasMoreInImageList(context)
    return next()
}


/**
 * Function for initializing state regarding a browsing session.
 * The following variable will be used:
 * "idstoread" - arrays with post ids to read in the future
 * "imagesread" - set with the ids of images downladed (to simulate a cache for not reading the same images every time)
 * "browsecount" - number of operations executed
 * "sessionuser" - name of the user for this session
 */
function startBrowse(context, events, done) {
	context.vars.idstoread = []
	context.vars.readimages = new Set()
	context.vars.browsecount = 0
	context.vars.sessionuser = userNames.sample()
	return done()
}

/**
 * Function that controls whether the session continues or not.
 * The session will continue if there is any post id to read and with a decreasing probability.
 */
function endBrowse(context, next) {
	const continueLooping = random(100) > context.vars.browsecount
	return next(context.vars.idstoread.length > 0 && continueLooping)
}




/**
 * Parse a list of post and select a random number of posts to check next.
 * Also decide whether to like or reply to the current post
 * Select next post to read and store information in the following variables:
 * "curid" - identifier of the current post
 * "nextid" - id of the next post to browse
 * "hasNextid" - true/false whether there is any other post to browse next
 * "nextimageid" - id of the next image to download
 * "hasNextimageid" - true/false whether there is any other image to download next
 * "browsecount" - update information on the session size
 * "like" - true/false whether should like post
 * "reply" - true/false whether should reply to post
 */
function selectFromPostList(requestParams, response, context, ee, next) {
	if( response.body && response.body.length > 0) {
		let resp = JSON.parse(response.body)
		if( typeof resp.posts !== 'undefined') {
			let num = random(resp.posts.length / 2)
			var i
			for( i = 0 ; i < num; i ++) {
				let pp = resp.posts.sample()
				context.vars.idstoread.push([pp.id,pp.community])
			}
			context.vars.postlistimages = []
			for( i = 0; i < resp.posts.length; i++) {
				if( resp.posts[i].image !== "" && ! context.vars.readimages.has(resp.posts[i].image))
					context.vars.postlistimages.push(resp.posts[i].image)
			}
			checkHasMoreInImageList(context)
		} else {
			context.vars.hasNextimageid = false
		}
	}
    delete context.vars.like
    delete context.vars.reply
	if( context.vars.idstoread.length > 0) {
		context.vars.curid = context.vars.nextid
		context.vars.curcommunity = context.vars.nextid
		let pp = context.vars.idstoread.splice(-1,1)[0]
		context.vars.nextid = pp[0]
		context.vars.nextcommunity = pp[1]
	    context.vars.hasNextid = true
	    context.vars.browsecount++
	} else {
		context.vars.hasNextid = false
	}
    return next()
}

/**
 * Parse a thread of post and select a random number of posts to check next.
 * Also decide whether to like or reply to the current post
 * Select next post to read and store information in the following variables:
 * "curid" - identifier of the current post
 * "nextid" - id of the next post to browse
 * "hasNextid" - true/false whether there is any other post to browse next
 * "nextimageid" - id of the next image to download
 * "hasNextimageid" - true/false whether there is any other image to download next
 * "browsecount" - update information on the session size
 * "like" - true/false whether should like post
 * "reply" - true/false whether should reply to post
 */
function selectFromPostThread(requestParams, response, context, ee, next) {
	if( response.body && response.body.length > 0) {
		let resp = JSON.parse( response.body)
		if( typeof resp.posts !== 'undefined' && resp.posts.length  > 0) {
			let num = random(resp.posts.length / 2)
			var i
			for( i = 0 ; i < num; i ++) {
				let pp = resp.posts.sample()
				context.vars.idstoread.push([pp.id,pp.community])
			}
			context.vars.postlistimages = []
			if( resp.post.image !== "" && ! context.vars.readimages.has(resp.post.image))
				context.vars.postlistimages.push(resp.post.image)
//			for( i = 0; i < resp.posts.length; i++) {
//				if( resp.posts[i].image !== "" && ! context.vars.readimages.has(resp.posts[i].image))
//					context.vars.postlistimages.push(resp.posts[i].image)
//			}
			checkHasMoreInImageList(context)
		} else {
			context.vars.hasNextimageid = false
		}
		if( typeof resp.post !== 'undefined') {
			context.vars.curcommunity = resp.post.community
		} else {
			context.vars.curcommunity = communityNames.sample()
		}
		if( random(100) < 33) {
		    context.vars.like = true
		} else 
		    delete context.vars.like
		if( random(100) < 25) {
		    context.vars.reply = true
		} else 
		    delete context.vars.reply
	} else {
	    delete context.vars.like 
	    delete context.vars.reply
	}
	if( context.vars.idstoread.length > 0) {
		context.vars.curid = context.vars.nextid
		context.vars.curcommunity = context.vars.nextcommunity
		let pp = context.vars.idstoread.splice(-1,1)[0]
		context.vars.nextid = pp[0]
		context.vars.nextcommunity = pp[1]
	    context.vars.hasNextid = true
	    context.vars.browsecount++
	} else {
		context.vars.curid = context.vars.nextid
		context.vars.curcommunity = context.vars.nextcommunity
		context.vars.hasNextid = false
	}
    return next()
}

/**
 * Parse a list of post and select all posts to check next.
 * Select next post to read and store information in the following variables:
 * "curid" - identifier of the current post
 * "nextid" - id of the next post to browse
 * "community" - community of the next post to browse
 * "hasNextid" - true/false whether there is any other post to browse next
 * "browsecount" - update information on the session size
 */
function selectAllFromPostList(requestParams, response, context, ee, next) {
	if( response.body && response.body.length > 0) {
		var resp = JSON.parse( response.body)
		var i
		for( i = 0 ; i < resp.posts.length; i ++) {
			context.vars.idstoread.push([resp.posts[i].id,resp.posts[i].community])
		}
		context.vars.postlistimages = []
		for( i = 0; i < resp.posts.length; i++) {
			if( resp.posts[i].image !== "" && ! context.vars.readimages.has(resp.posts[i].image))
				context.vars.postlistimages.push(resp.posts[i].image)
		}
		checkHasMoreInImageList(context)
	}
    delete context.vars.like
    delete context.vars.reply
	if( context.vars.idstoread.length > 0) {
		let pp = context.vars.idstoread.splice(-1,1)[0]
		context.vars.nextid = pp[0]
		context.vars.nextcommunity = pp[1]
	    context.vars.hasNextid = true
	    context.vars.browsecount++
	} else {
		context.vars.hasNextid = false
	}
    return next()
}

