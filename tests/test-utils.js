'use strict';

module.exports = {
  genNewUser,
  genNewUserReply,
  genNewCommunity,
  genNewPost,
  genNewPostReply,
  setNewPostBody,
  genNewImageReply
};


const Faker = require('faker');
const fs = require('fs');
const fetch = require('node-fetch');

var userNames = [] 
var userIds = []
var communityNames = []
var postIds = []
var images = []

Array.prototype.sample = function(){
	   return this[Math.floor(Math.random()*this.length)];
}

function genNewUser(userContext, events, done) {
  const name = `${Faker.name.firstName()}.${Faker.name.lastName()}`;
  userContext.vars.name = name;
  userNames.push(name)
  fs.writeFileSync('usernames.data', JSON.stringify(userNames));
  return done();
}


function genNewUserReply(requestParams, response, context, ee, next) {
	userIds.push(response.body)
  fs.writeFileSync('userids.data', JSON.stringify(userIds));
    return next()
}

function genNewCommunity(userContext, events, done) {
	const name = `s/${Faker.lorem.word()}`;
	userContext.vars.name = name;
	communityNames.push(name);
	fs.writeFileSync('communitynames.data', JSON.stringify(communityNames));
	return done()
}

function loadData() {
	if( userNames.length > 0)
		return;
	var str = fs.readFileSync('usernames.data','utf8')
	userNames = JSON.parse(str)	
	str = fs.readFileSync('userids.data','utf8')
	userIds = JSON.parse(str)	
	str = fs.readFileSync('communitynames.data','utf8')
	communityNames = JSON.parse(str)
	var i
	for( i = 1; i <= 40 ; i++) 
		images.push( fs.readFileSync('/images/cats.' + i + '.jpeg'))
}

function genNewPost(userContext, events, done) {
	loadData();
	userContext.vars.community = communityNames.sample();
	userContext.vars.creator = userNames.sample();
	userContext.vars.msg = `${Faker.lorem.paragraph()}`;
	if( postIds.length > 0 && Math.random() < 0.8) {  // 80% are replies
		userContext.vars.parentId = postIds.sample();
	} else {
		userContext.vars.parentId = null
	}
	userContext.vars.parentId = null
	userContext.vars.hasImage = false 
	if(Math.random() < 1) {   // 20% of the posts have images
		userContext.vars.image = images.sample()
		userContext.vars.hasImage = true 
	}
	return done()
}

function setNewPostBody(requestParams, context, ee, next) {
	requestParams.body = context.vars.image
	return next()
}

function genNewImageReply(requestParams, response, context, ee, next) {
	context.vars.imageId = response.body
    return next()
}


function genNewPostReply(requestParams, response, context, ee, next) {
	postIds.push(response.body)
	console.log(response.body)
    return next()
}


